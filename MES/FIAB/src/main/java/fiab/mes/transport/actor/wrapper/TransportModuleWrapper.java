package fiab.mes.transport.actor.wrapper;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import fiab.mes.machine.msg.MachineEvent;
import fiab.mes.machine.msg.MachineStatus;
import fiab.mes.machine.msg.MachineStatusUpdateEvent;
import fiab.mes.machine.msg.MachineUpdateEvent;
import fiab.mes.opcua.Subscription;
import fiab.mes.transport.MachineLevelEventBus;
import fiab.mes.transport.actor.turntable.TransportModuleActor;
import fiab.mes.transport.mockClasses.Direction;

public class TransportModuleWrapper implements TransportModuleWrapperInterface {

	private Subscription subscription;
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private OpcUaClient client;
	private List<String> nodes = new ArrayList<String>();
	private MachineLevelEventBus eventBus;

	public TransportModuleWrapper(String serverAddress, MachineLevelEventBus eventBus) {
		this.eventBus = eventBus;
		subscription = new Subscription(eventBus, serverAddress);
		try {
			client = createClient();
			subscription.setClient(client);

		} catch (Exception e) {
			System.out.println("Exception thrown by: ");
			e.printStackTrace();
		}

	}

	@Override
	public void transport(Direction from, Direction to, String orderId) throws InterruptedException {
		System.out.println("Transport started!");
		eventBus.publish(new MachineStatusUpdateEvent("Server", "STATUS" // Status is the nodeId
				, MachineEvent.MachineEventType.UPDATED.toString(), "", MachineStatus.STARTING));

		Thread.sleep(5000);

		eventBus.publish(new MachineStatusUpdateEvent("Server", "STATUS" // Status is the nodeId
				, MachineEvent.MachineEventType.UPDATED.toString(), "", MachineStatus.EXECUTE));
		
		Thread.sleep(5000);

		eventBus.publish(new MachineStatusUpdateEvent("Server", "STATUS" // Status is the nodeId
				, MachineEvent.MachineEventType.UPDATED.toString(), "", MachineStatus.COMPLETING));
		
		Thread.sleep(5000);

		eventBus.publish(new MachineStatusUpdateEvent("Server", "STATUS" // Status is the nodeId
				, MachineEvent.MachineEventType.UPDATED.toString(), "", MachineStatus.COMPLETE));
		
		Thread.sleep(5000);

		eventBus.publish(new MachineStatusUpdateEvent("Server", "STATUS" // Status is the nodeId
				, MachineEvent.MachineEventType.UPDATED.toString(), "", MachineStatus.RESETTING));

		Thread.sleep(5000);

		eventBus.publish(new MachineStatusUpdateEvent("Server", "STATUS" // Status is the nodeId
				, MachineEvent.MachineEventType.UPDATED.toString(), "", MachineStatus.IDLE));

	}

	@Override
	public void stopp() {
		eventBus.publish(new MachineStatusUpdateEvent("Server", "STATUS" // Status is the nodeId
				, MachineEvent.MachineEventType.UPDATED.toString(), "", MachineStatus.STOPPING));
		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		eventBus.publish(new MachineStatusUpdateEvent("Server", "STATUS" // Status is the nodeId
				, MachineEvent.MachineEventType.UPDATED.toString(), "", MachineStatus.STOPPED));
	}

	@Override
	public void reset() {
		eventBus.publish(new MachineStatusUpdateEvent("Server", "STATUS" // Status is the nodeId
				, MachineEvent.MachineEventType.UPDATED.toString(), "", MachineStatus.RESETTING));
		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		eventBus.publish(new MachineStatusUpdateEvent("Server", "STATUS" // Status is the nodeId
				, MachineEvent.MachineEventType.UPDATED.toString(), "", MachineStatus.IDLE));

	}

	/**
	 * The DataValue is written onto the server
	 * 
	 * @param nodeId
	 * @param value
	 */
	public void writeToServer(NodeId nodeId, DataValue value) {
		client.writeValue(nodeId, value);
	}

	/**
	 * This method is called as an update request. The value which is returned is
	 * taken directly from the server
	 * 
	 * @param actor
	 * @param nodeId
	 */
	public void update(String nodeId) {
		eventBus.publish(new MachineStatusUpdateEvent("Server", nodeId, MachineEvent.MachineEventType.UPDATED.toString(), "",
				subscription.getUppdate(nodeIdFromString(nodeId))), ActorRef.noSender());
	}

	public NodeId nodeIdFromString(String nodeId) {
		// TODO implement this method!
		return null;
	}

	/**
	 * This method allows an actor to subscribe on a nodeId on the eventbus, it acts
	 * a little differently depending on which actor is passed on (if the
	 * TransportModuleActor wants to subscribe this method has to do some extra
	 * steps) This doesn't have to be accounted for when using this method, except
	 * for if you use this method differently (if you want a new actor to subscribe,
	 * look into this method!)
	 * 
	 * @param actor  pass the actor which should recieve the updates
	 * @param nodeId this is the topic aka nodeId
	 * @return
	 */
	public boolean subscribe(ActorRef actor, String nodeId) {
		String subscribeNode = "";
		// Since this is the wrapper for the
		// TransportModuleActor it needs to translate the message to a ServerNode
		if (actor.getClass().equals(TransportModuleActor.class)) {
			for (String node : nodes) {
				if (node.contains(nodeId))
					subscribeNode = node;
			}
			logger.info(actor.toString() + " subscribed to " + subscribeNode);
			return subscription.subscribeToEventBus(actor, subscribeNode);

			// If this wrapper is used by other wrappers then the message is already
			// translated to a ServerNode
		} else {
			return subscription.subscribeToEventBus(actor, nodeId);
		}
	}

	public void unsubscribe(ActorRef actor) {
		subscription.unsubscribe(actor);
	}

	public void unsubscribe(ActorRef actor, String topic) {
		subscription.unsubscribe(actor, topic);
	}

	/**
	 * This adds nodes to the subscription and therefore to the EventBus of the
	 * Subscription.
	 * 
	 * @param nodes Names of nodes
	 */
	public void addNodeForSubscription(String... nodes) {
		for (String node : nodes) {
			try {
				subscription.runForSubscription(node);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// Needs following imports:
	/*
	 * org.eclipse.milo.examples.client.KeyStoreLoader
	 * org.eclipse.milo.examples.client.KeyStoreLoader
	 * org.eclipse.milo.opcua.stack.core.security.SecurityPolicy.
	 * getSecurityPolicyUri
	 * 
	 */
	private OpcUaClient createClient() throws Exception {
		File securityTempDir = new File(System.getProperty("java.io.tmpdir"), "security");
		if (!securityTempDir.exists() && !securityTempDir.mkdirs()) {
			throw new Exception("unable to create security dir: " + securityTempDir);
		}
		LoggerFactory.getLogger(getClass()).info("security temp dir: {}", securityTempDir.getAbsolutePath());

		KeyStoreLoader loader = new KeyStoreLoader().load(securityTempDir);

		SecurityPolicy securityPolicy = subscription.getSecurityPolicy();

		EndpointDescription[] endpoints;

		try {
			endpoints = UaTcpStackClient.getEndpoints(subscription.getEndpointUrl()).get();
		} catch (Throwable ex) {
			// try the explicit discovery endpoint as well
			String discoveryUrl = subscription.getEndpointUrl() + "/discovery";
			logger.info("Trying explicit discovery URL: {}", discoveryUrl);
			endpoints = UaTcpStackClient.getEndpoints(discoveryUrl).get();
		}

		EndpointDescription endpoint = Arrays.stream(endpoints)
				.filter(e -> e.getSecurityPolicyUri().equals(securityPolicy.getSecurityPolicyUri())).findFirst()
				.orElseThrow(() -> new Exception("no desired endpoints returned"));

		logger.info("Using endpoint: {} [{}]", endpoint.getEndpointUrl(), securityPolicy);

		OpcUaClientConfig config = OpcUaClientConfig.builder()
				.setApplicationName(LocalizedText.english("eclipse milo opc-ua client"))
				.setApplicationUri("urn:eclipse:milo:examples:client").setCertificate(loader.getClientCertificate())
				.setKeyPair(loader.getClientKeyPair()).setEndpoint(endpoint)
				.setIdentityProvider(subscription.getIdentityProvider()).setRequestTimeout(uint(5000)).build();

		return new OpcUaClient(config);
	}

}
