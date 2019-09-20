package fiab.mes.opcua;

import static com.google.common.collect.Lists.newArrayList;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

// connects to the french methods from visualStudio.git/methodDemos

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoringParameters;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import fiab.mes.transport.MachineLevelEventBus;
import fiab.mes.transport.msg.MachineConnectedEvent;
import fiab.mes.transport.msg.MachineUpdateEvent;
import miloBasics.org.eclipse.milo.examples.client.ClientExample;

public class Subscription implements ClientExample {
	private String serverAddress;
	private MachineLevelEventBus eventbus;
	private OpcUaClient client;

	@Override
	public String getEndpointUrl() {
		return serverAddress;
	}

	public Subscription(MachineLevelEventBus eventbus, String serveraddress) {
		this.eventbus = eventbus;
		this.serverAddress = serveraddress;
		System.out.println("Subscription server name: " + serverAddress);
	}
	
	public void setClient(OpcUaClient client) {
		this.client = client;
		try {
			client.connect().get();
			eventbus.publish(new MachineConnectedEvent("Client"), ActorRef.noSender());
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final AtomicLong clientHandles = new AtomicLong(1L);

	@Override
	public void run(OpcUaClient client, CompletableFuture<OpcUaClient> future) throws Exception {
//		System.out.println("WRONG METHOD CALLED ON ATS");
//		// synchronous connect
//		this.client = client;
//		client.connect().get();
//
//		// create a subscription @ 1000ms
//		UaSubscription subscription = client.getSubscriptionManager().createSubscription(100.0).get();
//
//		// subscribe to the Value attribute of the server's CurrentTime node
//		/*
//		 * ReadValueId readValueId = new ReadValueId(
//		 * Identifiers.Server_ServerStatus_CurrentTime, AttributeId.Value.uid(), null,
//		 * QualifiedName.NULL_VALUE);
//		 */
//
//		// Trying to run this with multiple subscriptions:
//		UInteger clientHandle = uint(clientHandles.getAndIncrement());
//		MonitoringParameters parameters = new MonitoringParameters(clientHandle, 1000.0, // sampling interval
//				null, // filter, null means use default
//				uint(10), // queue size
//				true // discard oldest
//		);
//
//		List<MonitoredItemCreateRequest> requests = new ArrayList<MonitoredItemCreateRequest>();
//
//		for (String s : nodeAddress) {
//			ReadValueId rvi = new ReadValueId(new NodeId(2, s), AttributeId.Value.uid(), null,
//					QualifiedName.NULL_VALUE);
//			MonitoredItemCreateRequest request = new MonitoredItemCreateRequest(rvi, MonitoringMode.Reporting,
//					parameters);
//			requests.add(request);
//		}
//
//		BiConsumer<UaMonitoredItem, Integer> onItemCreated = (item, id) -> item
//				.setValueConsumer(this::onSubscriptionValue);
//
//		List<UaMonitoredItem> items = subscription
//				.createMonitoredItems(TimestampsToReturn.Both, requests, onItemCreated).get();
//
//        
//        ReadValueId readValueId = new ReadValueId(
//                new NodeId(2, nodeAddress),
//                AttributeId.Value.uid(), null, QualifiedName.NULL_VALUE);
//        System.out.println(readValueId);
//
//        // important: client handle must be unique per item
//        UInteger clientHandle = uint(clientHandles.getAndIncrement());
//
//        MonitoringParameters parameters = new MonitoringParameters(
//            clientHandle,
//            1000.0,     // sampling interval
//            null,       // filter, null means use default
//            uint(10),   // queue size
//            true        // discard oldest
//        );
//
//        MonitoredItemCreateRequest request = new MonitoredItemCreateRequest(
//            readValueId, MonitoringMode.Reporting, parameters);
//
//        // when creating items in MonitoringMode.Reporting this callback is where each item needs to have its
//        // value/event consumer hooked up. The alternative is to create the item in sampling mode, hook up the
//        // consumer after the creation call completes, and then change the mode for all items to reporting.
//        BiConsumer<UaMonitoredItem, Integer> onItemCreated =
//            (item, id) -> item.setValueConsumer(this::onSubscriptionValue);
//
//        List<UaMonitoredItem> items = subscription.createMonitoredItems(
//            TimestampsToReturn.Both,
//            newArrayList(request),
//            onItemCreated
//        ).get();
//
//		for (UaMonitoredItem item : items) {
//			if (item.getStatusCode().isGood()) {
//            	System.out.println("item created for nodeId=" + item.getReadValueId().getNodeId());
//				logger.info("item created for nodeId={}", item.getReadValueId().getNodeId());
//			} else {
//				logger.warn("failed to create item for nodeId={} (status={})", item.getReadValueId().getNodeId(),
//						item.getStatusCode());
//			}
//		}
//
//		while (running) {
//			Thread.sleep(1000);
//		}
//		future.complete(client);
	}
	
	public void runForSubscription(String nodeId) throws Exception {

				// create a subscription @ 1000ms
				UaSubscription subscription = client.getSubscriptionManager().createSubscription(100.0).get();

				// subscribe to the Value attribute of the server's CurrentTime node
				/*
				 * ReadValueId readValueId = new ReadValueId(
				 * Identifiers.Server_ServerStatus_CurrentTime, AttributeId.Value.uid(), null,
				 * QualifiedName.NULL_VALUE);
				 */
				ReadValueId readValueId = new ReadValueId(
		                new NodeId(2, nodeId),
		                AttributeId.Value.uid(), null, QualifiedName.NULL_VALUE);
		
		        // important: client handle must be unique per item
		        UInteger clientHandle = uint(clientHandles.getAndIncrement());
		
		        MonitoringParameters parameters = new MonitoringParameters(
		            clientHandle,
		            1000.0,     // sampling interval
		            null,       // filter, null means use default
		            uint(10),   // queue size
		            true        // discard oldest
		        );
		
		        MonitoredItemCreateRequest request = new MonitoredItemCreateRequest(
		            readValueId, MonitoringMode.Reporting, parameters);
		
		        // when creating items in MonitoringMode.Reporting this callback is where each item needs to have its
		        // value/event consumer hooked up. The alternative is to create the item in sampling mode, hook up the
		        // consumer after the creation call completes, and then change the mode for all items to reporting.
		        BiConsumer<UaMonitoredItem, Integer> onItemCreated =
		            (item, id) -> item.setValueConsumer(this::onSubscriptionValue);
		
		        List<UaMonitoredItem> items = subscription.createMonitoredItems(
		            TimestampsToReturn.Both,
		            newArrayList(request),
		            onItemCreated
		        ).get();

				for (UaMonitoredItem item : items) {
					if (item.getStatusCode().isGood()) {
						logger.info("item created for nodeId={}", item.getReadValueId().getNodeId());
					} else {
						logger.warn("failed to create item for nodeId={} (status={})", item.getReadValueId().getNodeId(),
								item.getStatusCode());
					}
				}
	}
	
	public DataValue getUppdate(NodeId nodeId) {
		try {
			return client.readValue(0, null, nodeId).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			return null;
		}
	}

	private void onSubscriptionValue(UaMonitoredItem item, DataValue value) {
		System.out.println("Change on: " + item.getReadValueId().getNodeId().toString() + " NEW VALUE: " + value.getValue().getValue().toString() );
		
		eventbus.publish(new MachineUpdateEvent("Server", value, item.getReadValueId().getNodeId().toString())); //TODO maybe change this form Server
		
		logger.info("subscription value received: item={}, value={}", item.getReadValueId().getNodeId(),
				value.getValue());
	}
	
	public boolean subscribeToEventBus(ActorRef actor, String topic) {
		return eventbus.subscribe(actor, topic);
	}
	
	public void unsubscribe(ActorRef actor, String topic) {
		eventbus.unsubscribe(actor, topic);
	}
	
	public void unsubscribe(ActorRef actor) {
		eventbus.unsubscribe(actor);
	}
	

}
