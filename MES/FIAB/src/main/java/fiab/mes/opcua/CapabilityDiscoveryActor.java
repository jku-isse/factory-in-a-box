package fiab.mes.opcua;

import static fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITIES;
import static fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITY;
import static fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames.ID;
import static fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE;
import static fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE_VALUE_PROVIDED;
import static fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE_VALUE_REQUIRED;
import static fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames.TYPE;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.api.nodes.Node;
import org.eclipse.milo.opcua.sdk.client.nodes.UaObjectNode;
import org.eclipse.milo.opcua.sdk.client.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;

import akka.actor.AbstractActor;
import akka.actor.ActorKilledException;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.mes.opcua.CapabilityImplementationMetadata.MetadataInsufficientException;
import fiab.mes.opcua.CapabilityImplementationMetadata.ProvOrReq;
import fiab.opcua.CapabilityImplInfo;
import fiab.opcua.client.ClientKeyStoreLoader;
import fiab.opcua.client.OPCUAClientFactory;

public class CapabilityDiscoveryActor extends AbstractActor {

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

	public enum DISCOVERY_STATUS {
		IDLE, CONNECTING, CONNECTED, IN_PROGRESS, FAILED, COMPLETED_WITH_SPAWN, COMPLETED_WITHOUT_SPAWN
	};


	public static class BrowseRequest {
		String endpointURL;
		Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning;

		public BrowseRequest(String endpointURL, Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning) {
			super();
			this.endpointURL = endpointURL;
			this.capURI2Spawning = capURI2Spawning;
		}

	}

	private DISCOVERY_STATUS status = DISCOVERY_STATUS.IDLE;
	private ActorRef self;

	private ActorRef spawner = null;
	private BrowseRequest req;
	private OpcUaClient client;
	
	static public Props props() {
		return Props.create(CapabilityDiscoveryActor.class, () -> new CapabilityDiscoveryActor());
	}

	public CapabilityDiscoveryActor() {
		this.self = getSelf();
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(BrowseRequest.class, req -> {
					if (this.status.equals(DISCOVERY_STATUS.IDLE) ) {
						this.status = DISCOVERY_STATUS.CONNECTING;
						this.req = req;
						connectToServer(req);
					}
				} )
				.match(ActorKilledException.class, ex -> {
					log.info("Received KillException from: "+ex.getMessage());
				})
				.build();
	}


	private void connectToServer(BrowseRequest req) {
		try {
			client = new OPCUAClientFactory().createClient(req.endpointURL);
			client.connect().get();
			this.status = DISCOVERY_STATUS.CONNECTED;
			log.info("Connected to "+req.endpointURL);
			getActorCapabilities(req, client, Identifiers.RootFolder);
			if (this.status.equals(DISCOVERY_STATUS.IN_PROGRESS)) {
				log.info("No Capabilities found for which an ActorSpawnerActor was registered");
				this.status = DISCOVERY_STATUS.COMPLETED_WITHOUT_SPAWN;
				// no need to check a spawner actor
			} else if (this.status.equals(DISCOVERY_STATUS.COMPLETED_WITH_SPAWN)) {
				// check if spawner is alive
				//checkSpawnerAlive();
			}
		} catch (Exception e) {
			log.warning("Error connecting to "+req.endpointURL+" with error: "+e.getMessage());
			this.status = DISCOVERY_STATUS.FAILED;
			tryReconnectInXseconds(req);
		}

	}		

	private void tryReconnectInXseconds(BrowseRequest req) {
		context().system()
		.scheduler()
		.scheduleOnce(Duration.ofSeconds(60), 
				new Runnable() {
			@Override
			public void run() {
				status = DISCOVERY_STATUS.CONNECTING;
				connectToServer(req);
			}
		}, context().system().dispatcher());
	}


	private void getActorCapabilities(BrowseRequest req, OpcUaClient client, NodeId rootNode) {
		this.status = DISCOVERY_STATUS.IN_PROGRESS;
		try {
			List<Node> nodes = client.getAddressSpace().browse(rootNode).get();
			for (Node n : nodes) {
			//	log.info("Checking node: "+n.getNodeId().get().toParseableString());
				if (n instanceof UaObjectNode) {
					if (isCapabilitiesFolder(n)) {	// then the rootNode is the actorNode				
						browseCapabilitiesFolder(req, client, n, rootNode);
						return; // and we quit browsing here, upon the first toplevel one
					} else {
						if (this.status.equals(DISCOVERY_STATUS.IN_PROGRESS)) // we only dive deeper if not yet found an capabilities folder
							getActorCapabilities(req, client, n.getNodeId().get());
					}
				}
			}			
		} catch (Exception e) {
			log.warning("Error browsing "+req.endpointURL+" with error: "+e.getMessage());
			if (this.status.equals(DISCOVERY_STATUS.COMPLETED_WITH_SPAWN)) {// we spawned an actor then failed, thus we wont retry
				log.warning("But we spawned at least one ActorSpawnerActor, thus not retrying");
			} else {
				this.status = DISCOVERY_STATUS.FAILED;
				tryRebrowseInXseconds(req, client);
			}
		}
	}

//	private void checkSpawnerAlive() {
//		context().system()
//		.scheduler()
//		.scheduleOnce(Duration.ofSeconds(60), 
//				new Runnable() {
//			@Override
//			public void run() {
//				status = DISCOVERY_STATUS.CONNECTING;
//				connectToServer(req);
//			}
//		}, context().system().dispatcher());
//	}
	
	private void tryRebrowseInXseconds(BrowseRequest req, OpcUaClient client) {
		context().system()
		.scheduler()
		.scheduleOnce(Duration.ofSeconds(60), 
				new Runnable() {
			@Override
			public void run() {
				status = DISCOVERY_STATUS.IN_PROGRESS;
				getActorCapabilities(req, client, Identifiers.RootFolder);
			}
		}, context().system().dispatcher());
	}

	private boolean isCapabilitiesFolder(Node n) throws InterruptedException, ExecutionException {
		QualifiedName bName = n.getBrowseName().get();				
		if (bName.getName().equalsIgnoreCase(CAPABILITIES)) {
			log.info("Found Capabilities Node with id: "+n.getNodeId().get().toParseableString());
			return true;			
		} else
			return false;
	}


	private void browseCapabilitiesFolder(BrowseRequest req, OpcUaClient client, Node node, NodeId actorNode) throws InterruptedException, ExecutionException {
		NodeId browseRoot = node.getNodeId().get();
		List<Node> nodes = client.getAddressSpace().browse(browseRoot).get();			
		for (Node n : nodes) {
			if (n instanceof UaObjectNode) {
				if (isCapabilityFolder(n)) {
					try {
						CapabilityImplementationMetadata capMeta = getCapabilityURI(client, n);
						log.info("Found: "+capMeta.toString());
						//if capability registered, spawn ActorSpawner and return
						AbstractMap.SimpleEntry<String, ProvOrReq> foundEntry = new SimpleEntry<String, ProvOrReq>(capMeta.getCapabilityURI(), capMeta.getProvOrReq());
						Optional.ofNullable(req.capURI2Spawning.get(foundEntry)).ifPresent(spawningEP -> {
							spawner = spawningEP.createActorSpawner(getContext());
							log.info("Creating ActorSpawner for Capability Type: "+capMeta.capabilityURI );							
							spawner.tell(new CapabilityCentricActorSpawnerInterface.SpawnRequest(new CapabilityImplInfo(client, req.endpointURL, actorNode, browseRoot, capMeta.capabilityURI)), self);
							this.status = DISCOVERY_STATUS.COMPLETED_WITH_SPAWN;
						});						
					} catch (MetadataInsufficientException e) {
						log.warning("Ignoring Capability Implementation information due to insufficient child fields in OPC-UA Node "+node.getNodeId().get().toParseableString());
						// continue searching for others
					}									
				} else {				
					// we stop looking here, as there should not be anything inside the capabilities node hierarchy aside from capability definitions
				}
				// we spawn an actor for each registered capabilityImpl that we find
			}
		}
//		if (!this.status.equals(DISCOVERY_STATUS.COMPLETED_WITH_SPAWN))
//			this.status = DISCOVERY_STATUS.COMPLETED_WITHOUT_SPAWN;
	}

	private boolean isCapabilityFolder(Node n) throws InterruptedException, ExecutionException {
		String bName = n.getBrowseName().get().getName();
		//if (bName.equalsIgnoreCase(CAPABILITY)) {
		if (bName.startsWith(CAPABILITY)) { // currently FORTE/4DIAC cannot have two sibling nodes with the same browsename, thus there are numbers prepended which we ignore here
			log.info("Found Capability Node with id: "+n.getNodeId().get().toParseableString());
			return true;			
		} else
			return false;
	}

	private CapabilityImplementationMetadata getCapabilityURI(OpcUaClient client, Node node) throws InterruptedException, ExecutionException, MetadataInsufficientException  {
		List<Node> nodes = client.getAddressSpace().browse(node.getNodeId().get()).get();
		ProvOrReq provOrReq = null;
		String implId = null;
		String uri = null;
		for (Node n : nodes) {						
			if (n instanceof UaVariableNode) {
				UaVariableNode var = (UaVariableNode) n;
				String type = n.getBrowseName().get().getName();
				switch (type) {
				case ID:
					implId = (String) var.getValue().get();
					break;
				case TYPE:
					uri = var.getValue().get().toString();					
					break;
				case ROLE:
					String role = (String) var.getValue().get();
					if (role.equalsIgnoreCase(ROLE_VALUE_PROVIDED))
						provOrReq = ProvOrReq.PROVIDED;
					else if (role.equalsIgnoreCase(ROLE_VALUE_REQUIRED))
						provOrReq = ProvOrReq.REQUIRED;
					else
						log.warning("Discovered Role does not match Required or Provided in capability "+node.getNodeId().get().toParseableString());
					// we assume provided
					provOrReq = ProvOrReq.PROVIDED;
					break;				
				}
			}			
		}
		CapabilityImplementationMetadata capMeta = new CapabilityImplementationMetadata(implId, uri, provOrReq);
		return capMeta;
	}

	private OpcUaClientConfig getClientConfig(ClientKeyStoreLoader loader, EndpointDescription endpoint) {
		return OpcUaClientConfig.builder().setApplicationName(LocalizedText.english("My Client"))
				.setApplicationUri(String.format("urn:example-client:%s", UUID.randomUUID()))
				.setCertificate(loader.getClientCertificate()).setKeyPair(loader.getClientKeyPair())
				.setEndpoint(endpoint).setRequestTimeout(uint(60000)).build();
	}

}
