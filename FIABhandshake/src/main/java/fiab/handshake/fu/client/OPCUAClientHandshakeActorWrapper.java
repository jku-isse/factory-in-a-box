package fiab.handshake.fu.client;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoringParameters;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;

import com.google.common.collect.Lists;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.core.capabilities.handshake.HandshakeCapability.ServerSideStates;
import fiab.core.capabilities.handshake.IOStationCapability;

public class OPCUAClientHandshakeActorWrapper extends AbstractActor {

	private ActorRef localActor;
	private ActorRef self;
	
	private ServerHandshakeNodeIds nodeIds;
	
	private LoggingAdapter logger = Logging.getLogger(getContext().getSystem(), this);	
	private boolean isSubscribed = false;
	private boolean hasValidNodeIds = false;
	
	static public Props props() {	    
		return Props.create(OPCUAClientHandshakeActorWrapper.class, () -> new OPCUAClientHandshakeActorWrapper());
	}
	
	public OPCUAClientHandshakeActorWrapper() {
		super();		
		this.self = self();
	}


	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(ServerHandshakeNodeIds.class, nodeIds -> {
					setNewNodeIds(nodeIds);
				})
				.match(IOStationCapability.ServerMessageTypes.class, req -> {
					if (!hasValidNodeIds) {
						logger.warning("Client Wrapper has no valid nodeids");
						return;
					}
					switch(req) {
					// local requests, not used here
					case Complete:
					case Reset:
					case Stop:
					//responses, that we receive from remote and forward locally
					case NotOkResponseInitHandover:
					case NotOkResponseStartHandover:
					case OkResponseInitHandover:
					case OkResponseStartHandover:
						break;
						
					//from local actor to remote
					case RequestInitiateHandover:
						init();
						break;
					case RequestStartHandover:
						start();
						break;										
					case SubscribeToStateUpdates:
						if (!isSubscribed)
							subscribeToStatus();
						break;
					case UnsubscribeToStateUpdates:
						if (isSubscribed)
							unsubscribeFromStatus();
						break;
					default:
						break;					
					}
				})
				.match(ActorRef.class, actorRef -> {localActor = actorRef;}) 
				.build();
	}

	private void unsubscribeFromStatus() {
		nodeIds.getClient().getSubscriptionManager().clearSubscriptions();
		isSubscribed = false;
		logger.info("Cleared Subscriptions");
	}

	private CompletableFuture<String> callMethod(NodeId methodId) {

		CallMethodRequest request = new CallMethodRequest(
				nodeIds.getCapabilityImplNode(), methodId, new Variant[]{});

		return nodeIds.getClient().call(request).thenCompose(result -> {
			StatusCode statusCode = result.getStatusCode();

			if (statusCode.isGood()) {								
				int len = result.getOutputArguments() != null ? result.getOutputArguments().length : -1;
				if (len > 0 ) {
					String value = (String) (result.getOutputArguments())[0].getValue();
					return CompletableFuture.completedFuture(value);
				} else { // workaround here as FORTE doesn't return result in current i/o station version
					return CompletableFuture.completedFuture("OK");
				}
			} else {
				StatusCode[] inputArgumentResults = result.getInputArgumentResults();
				for (int i = 0; i < inputArgumentResults.length; i++) {
					logger.error("inputArgumentResults[{}]={}", i, inputArgumentResults[i]);
				}

				CompletableFuture<String> f = new CompletableFuture<>();
				f.completeExceptionally(new UaException(statusCode));
				return f;
			}
		});
	}
	
	private void setNewNodeIds(ServerHandshakeNodeIds nodeIds) {
//		if (this.nodeIds != null && this.nodeIds.getClient() != null) {
//			try {
//				this.nodeIds.getClient().disconnect().get(2, TimeUnit.SECONDS);
//			} catch (InterruptedException | ExecutionException | TimeoutException e) {				
//				e.printStackTrace();
//			}
//		} // DISCONNECT ALREADY DONE BEFORE, no need to do it here
		this.nodeIds = nodeIds;
		hasValidNodeIds = true;
	}


	public void init() {
		callMethod(nodeIds.getInitMethod()).exceptionally(ex -> {
			logger.warning("Exception Calling Init Method on OPCUA Node: "+nodeIds.getInitMethod().toParseableString() + ex.getMessage());
			ex.printStackTrace();
			localActor.tell(IOStationCapability.ServerMessageTypes.NotOkResponseInitHandover, self);
			return IOStationCapability.ServerMessageTypes.NotOkResponseInitHandover.toString();
		}).thenAccept(resp -> {
			logger.info("Called Init Method successfully on OPCUA Node: "+nodeIds.getInitMethod().toParseableString()+" with result "+resp);
			IOStationCapability.ServerMessageTypes respMsg = IOStationCapability.ServerMessageTypes.NotOkResponseInitHandover;
			try {
				if (resp.equals("OK")) {
					respMsg = IOStationCapability.ServerMessageTypes.OkResponseInitHandover;
				} else {
					respMsg = IOStationCapability.ServerMessageTypes.valueOf(resp);
				}
			} catch (java.lang.IllegalArgumentException e) {
				logger.error("Received Unknown State: "+e.getMessage());
			}
			localActor.tell(respMsg, self);
		});
	}


	public void start() {
		callMethod(nodeIds.getStartMethod()).exceptionally(ex -> {
			logger.warning("Exception Calling Start Method on OPCUA Node: "+nodeIds.getStartMethod().toParseableString(), ex);
			localActor.tell(IOStationCapability.ServerMessageTypes.NotOkResponseStartHandover, self);
			return IOStationCapability.ServerMessageTypes.NotOkResponseStartHandover.toString();
		}).thenAccept(resp -> {
			logger.info("Called Start Method successfully on OPCUA Node: "+nodeIds.getStartMethod().toParseableString()+" with result "+resp);
			IOStationCapability.ServerMessageTypes respMsg = IOStationCapability.ServerMessageTypes.NotOkResponseStartHandover;
			try {
				if (resp.equals("OK")) {
					respMsg = IOStationCapability.ServerMessageTypes.OkResponseStartHandover;
				} else {
					respMsg = IOStationCapability.ServerMessageTypes.valueOf(resp);
				}		
			} catch (java.lang.IllegalArgumentException e) {
				logger.error("Received Unknown State: "+e.getMessage());
			}
			localActor.tell(respMsg, self);
		});
	}

	public void subscribeToStatus() {
		// from: https://github.com/eclipse/milo/blob/release/0.3.7/milo-examples/client-examples/src/main/java/org/eclipse/milo/examples/client/SubscriptionExample.java
		try {
			UaSubscription subscription = nodeIds.getClient().getSubscriptionManager().createSubscription(100.0).get();
			ReadValueId readValueId = new ReadValueId(nodeIds.getStateVar(), AttributeId.Value.uid(), null, QualifiedName.NULL_VALUE);
			UInteger clientHandle = subscription.nextClientHandle();
			MonitoringParameters parameters = new MonitoringParameters(
					clientHandle,
					100.0,     // sampling interval
					null,       // filter, null means use default
					uint(10),   // queue size
					true        // discard oldest
					);
			MonitoredItemCreateRequest request = new MonitoredItemCreateRequest(
					readValueId,
					MonitoringMode.Reporting,
					parameters
					);
			BiConsumer<UaMonitoredItem, Integer> onItemCreated =
					(item, id) -> item.setValueConsumer(this::onStateSubscriptionChange);

					List<UaMonitoredItem> items = subscription.createMonitoredItems(
							TimestampsToReturn.Both,
							Lists.newArrayList(request),
							onItemCreated
							).get();
					for (UaMonitoredItem item : items) {
						if (item.getStatusCode().isGood()) {
							logger.info("item created for nodeId={}", item.getReadValueId().getNodeId());							
						} else {
							logger.warning(
									"failed to create item for nodeId={} (status={})",
									item.getReadValueId().getNodeId(), item.getStatusCode());
						}
					}
			isSubscribed = true;			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void onStateSubscriptionChange(UaMonitoredItem item, DataValue value) {
		logger.info(
				"subscription value received: item={}, value={}",
				item.getReadValueId().getNodeId(), value.getValue());
		if( value.getValue().isNotNull() ) {
			String stateAsString = value.getValue().getValue().toString();
			System.out.println(stateAsString);
			try {
				ServerSideStates state = ServerSideStates.valueOf(stateAsString);
				if (this.localActor != null) {
					localActor.tell(state, self);
				}
			} catch (java.lang.IllegalArgumentException e) {
				logger.error("Received Unknown State: "+e.getMessage());
			}
			
		}
	}
}
