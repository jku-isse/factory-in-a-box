package fiab.mes.machine.actor.iostation.wrapper;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import akka.actor.ActorRef;
import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerSide;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class IOStationOPCUAWrapper implements IOStationWrapperInterface {

	private static final Logger logger = LoggerFactory.getLogger(IOStationOPCUAWrapper.class);

	private InterMachineEventBus intraMachineBus;
	private OpcUaClient client;
	private NodeId capabilityImplNode;
	private NodeId stopMethod;
	private NodeId resetMethod;
	private NodeId stateVar;

	public IOStationOPCUAWrapper(InterMachineEventBus intraMachineBus, OpcUaClient client, NodeId capabilityImplNode,
			NodeId stopMethod, NodeId resetMethod, NodeId stateVar) {
		super();
		this.intraMachineBus = intraMachineBus;
		this.client = client;
		this.capabilityImplNode = capabilityImplNode;
		this.stopMethod = stopMethod;
		this.resetMethod = resetMethod;
		this.stateVar = stateVar;
		logger.info("IOStationOPCUAWrapper initialized");
	}

	//	private void init() throws InterruptedException, ExecutionException {
	//		client.connect().get();
	//	}

	private CompletableFuture<Boolean> callMethod(NodeId methodId) {

		CallMethodRequest request = new CallMethodRequest(
				capabilityImplNode, methodId, new Variant[]{});

		return client.call(request).thenCompose(result -> {
			StatusCode statusCode = result.getStatusCode();

			if (statusCode.isGood()) {
				return CompletableFuture.completedFuture(Boolean.TRUE);
			} else {
				StatusCode[] inputArgumentResults = result.getInputArgumentResults();
				for (int i = 0; i < inputArgumentResults.length; i++) {
					logger.error("inputArgumentResults[{}]={}", i, inputArgumentResults[i]);
				}

				CompletableFuture<Boolean> f = new CompletableFuture<>();
				f.completeExceptionally(new UaException(statusCode));
				return f;
			}
		});
	}

	@Override
	public void stop() {
		callMethod(stopMethod);
		logger.info("Called STOP Method on OPCUA Node: "+stopMethod.toParseableString());
	}

	@Override
	public void reset() {
		callMethod(resetMethod).exceptionally(ex -> {
			logger.warn("Called RESET Method on OPCUA Node: "+resetMethod.toParseableString(), ex);
			return false;
		}).thenAccept(v -> {
			if (v) 	logger.info("Called RESET Method successfully on OPCUA Node: "+resetMethod.toParseableString());
		});

	}

	@Override
	public void subscribeToStatus() {
		// from: https://github.com/eclipse/milo/blob/release/0.3.7/milo-examples/client-examples/src/main/java/org/eclipse/milo/examples/client/SubscriptionExample.java
		try {
		UaSubscription subscription = client.getSubscriptionManager().createSubscription(1000.0).get();
		ReadValueId readValueId = new ReadValueId(stateVar, AttributeId.Value.uid(), null, QualifiedName.NULL_VALUE);
		UInteger clientHandle = subscription.nextClientHandle();
		MonitoringParameters parameters = new MonitoringParameters(
				clientHandle,
				1000.0,     // sampling interval
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
						logger.warn(
								"failed to create item for nodeId={} (status={})",
								item.getReadValueId().getNodeId(), item.getStatusCode());
					}
				} 
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
				ServerSide state = ServerSide.valueOf(stateAsString);
				if (this.intraMachineBus != null) {
					intraMachineBus.publish(new IOStationStatusUpdateEvent("", "OPCUA State Endpoint has new State", state));
				}
			} catch (java.lang.IllegalArgumentException e) {
				logger.error("Received Unknown State: "+e.getMessage());
			}
			
		}
	}

	@Override
	public void subscribeToLoadStatus() {
		// TODO Auto-generated method stub

	}

}
