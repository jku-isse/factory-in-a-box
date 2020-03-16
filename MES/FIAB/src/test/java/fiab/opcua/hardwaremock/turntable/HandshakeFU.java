package fiab.opcua.hardwaremock.turntable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.nodes.Node;
import org.eclipse.milo.opcua.sdk.client.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.client.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import fiab.mes.opcua.CapabilityCentricActorSpawnerInterface.CapabilityImplInfo;
import fiab.mes.mockactors.MockClientHandshakeActor;
import fiab.mes.mockactors.MockServerHandshakeActor;
import fiab.mes.mockactors.transport.MockTransportModuleWrapper;
import fiab.mes.opcua.OPCUACapabilitiesWellknownBrowsenames;
import fiab.mes.opcua.OPCUAUtils;
import fiab.mes.transport.handshake.HandshakeProtocol;
import fiab.mes.transport.handshake.HandshakeProtocol.ClientSide;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerSide;
import fiab.opcua.hardwaremock.OPCUABase;
import fiab.opcua.hardwaremock.StatePublisher;
import fiab.opcua.hardwaremock.clienthandshake.OPCUAClientHandshakeActorWrapper;
import fiab.opcua.hardwaremock.clienthandshake.OPCUAClientHandshakeActorWrapper.ServerHandshakeNodeIds;
import fiab.opcua.hardwaremock.iostation.methods.InitHandover;
import fiab.opcua.hardwaremock.iostation.methods.StartHandover;
import fiab.opcua.hardwaremock.turntable.WiringUtils.WiringInfo;

public class HandshakeFU implements StatePublisher{

	private static final Logger logger = LoggerFactory.getLogger(HandshakeFU.class);
	
	// add wiring endpoint 
	// load wiring from file 
	// obtain wiring from opc-ua call
	// update wiring info in opc-ua model
	
	UaFolderNode rootNode;
	ActorRef ttBaseActor;
	ActorContext context;
	String capInstId;
	String fuPrefix;
	OPCUABase base;
	boolean isProvided;
	private org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode status = null;
	private ActorRef opcuaWrapper;
	private ActorRef localClient;
	
	public HandshakeFU(OPCUABase base, UaFolderNode root, String fuPrefix, ActorRef ttBaseActor, ActorContext context, String capInstId, boolean isProvided) {
		this.base = base;
		this.rootNode = root;
		this.ttBaseActor = ttBaseActor;
		this.context = context;
		this.capInstId = capInstId;
		this.fuPrefix = fuPrefix;
		this.isProvided = isProvided;
		setupOPCUANodeSet();
	}
	
	
	private void setupOPCUANodeSet() {
		String path = fuPrefix + "/HANDSHAKE_FU_"+capInstId;
		UaFolderNode handshakeNode = base.generateFolder(rootNode, fuPrefix, "HANDSHAKE_FU_"+capInstId);	
		
		// add method/variables to opcua
		if (isProvided) { // add all the methods available here, actor is the serverhandshake		
			status = base.generateStringVariableNode(handshakeNode, path, HandshakeProtocol.STATE_SERVERSIDE_VAR_NAME, ServerSide.STOPPED);
			localClient = context.actorOf(MockServerHandshakeActor.props(ttBaseActor, true, this));
			org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n1 = base.createPartialMethodNode(path, "INIT_HANDOVER", "Requests init");		
			base.addMethodNode(handshakeNode, n1, new InitHandover(n1, localClient)); 		
			org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n2 = base.createPartialMethodNode(path, "START_HANDOVER", "Requests start");		
			base.addMethodNode(handshakeNode, n2, new StartHandover(n2, localClient));
			// let parent Actor know, that there is a new endpoint
			ttBaseActor.tell(new MockTransportModuleWrapper.LocalServerEndpointStatus(localClient, isProvided, this.capInstId), ActorRef.noSender()); 			 
		} else {
			status = base.generateStringVariableNode(handshakeNode, path, HandshakeProtocol.STATE_CLIENTSIDE_VAR_NAME, ClientSide.STOPPED); 
			opcuaWrapper = context.actorOf(OPCUAClientHandshakeActorWrapper.props(), capInstId+"_OPCUAWrapper");
			localClient = context.actorOf(MockClientHandshakeActor.props(ttBaseActor, opcuaWrapper, this), capInstId);
			opcuaWrapper.tell(localClient, ActorRef.noSender());
			ttBaseActor.tell(new MockTransportModuleWrapper.LocalClientEndpointStatus(localClient, isProvided, this.capInstId), ActorRef.noSender()); 
		}
		
		// add capabilities 
		UaFolderNode capabilitiesFolder = base.generateFolder(handshakeNode, path, new String( OPCUACapabilitiesWellknownBrowsenames.CAPABILITIES));
		path = path +"/"+OPCUACapabilitiesWellknownBrowsenames.CAPABILITIES;
		UaFolderNode capability1 = base.generateFolder(capabilitiesFolder, path,
				"CAPABILITY",  OPCUACapabilitiesWellknownBrowsenames.CAPABILITY);
		
		base.generateStringVariableNode(capability1, path+"/CAPABILITY",  OPCUACapabilitiesWellknownBrowsenames.TYPE,
				new String(HandshakeProtocol.HANDSHAKE_CAPABILITY_URI));
		base.generateStringVariableNode(capability1, path+"/CAPABILITY",  OPCUACapabilitiesWellknownBrowsenames.ID,
				new String(capInstId));
		base.generateStringVariableNode(capability1, path+"/CAPABILITY",  OPCUACapabilitiesWellknownBrowsenames.ROLE,
				new String(isProvided ? OPCUACapabilitiesWellknownBrowsenames.ROLE_VALUE_PROVIDED : OPCUACapabilitiesWellknownBrowsenames.ROLE_VALUE_REQUIRED));
		
		
		
	}
	
	public void provideWiringInfo(WiringInfo info) throws Exception {
		
		// process wiring info --> create new opcua client, and recreate wrapper actor
		if (!isProvided) { // only if required endpoint, i.e., this is a client
			logger.info("Applying Wiring Info for required Capability: "+capInstId);	
			OpcUaClient client = new OPCUAUtils().createClient(info.getRemoteEndpointURL());
			client.connect().get();
			logger.info("OPCUA Client connected for FU: "+this.capInstId);
			Optional<NodeId> optRemoteNodeId = NodeId.parseSafe(info.getRemoteNodeId());
			if (optRemoteNodeId.isPresent()) {					
				logger.info("Searching for Grandparent Node for Capability: "+optRemoteNodeId.get().toParseableString());
				Optional<NodeId> optActorCapImplNodeId = getGrandParentForNodeIdViaBrowse(client, optRemoteNodeId.get(), Identifiers.RootFolder, null);
				if (optActorCapImplNodeId.isPresent()) {
					CapabilityImplInfo cii = new CapabilityImplInfo(info.getRemoteEndpointURL(), optActorCapImplNodeId.get(), optRemoteNodeId.get(), HandshakeProtocol.HANDSHAKE_CAPABILITY_URI);
					cii.setClient(client);
					ServerHandshakeNodeIds nodeIds = retrieveNodeIds(cii);
					if (!nodeIds.isComplete()) { 
						logger.error("OPCUA Client Endpoints incompletely resolved for FU: "+this.capInstId + " at "+optActorCapImplNodeId.get().toParseableString());
					} else {
						nodeIds.setClient(client);
						logger.info("OPCUA Client Endpoints resolved for FU: "+this.capInstId + " at "+optActorCapImplNodeId.get().toParseableString());
						opcuaWrapper.tell(nodeIds, ActorRef.noSender());
					}
					//TODO: update wiring info in opcua nodeset
				} else {
					logger.warn("Could not resolve actor cap impl for nodeId:" +info.getRemoteNodeId());
				}
			} else {
				logger.warn("Could not resolve nodeId:" +info.getRemoteNodeId());
			}
		} 
	}
	
	private ServerHandshakeNodeIds retrieveNodeIds(CapabilityImplInfo info) throws InterruptedException, ExecutionException {
		List<Node> nodes = info.getClient().getAddressSpace().browse(info.getActorNode()).get();		
		// we assume unique node names and method names within this hierarchy level (thus no two capabilities with overlapping browse names)
		ServerHandshakeNodeIds nodeIds = new ServerHandshakeNodeIds();
		for (Node n : nodes) {
			//logger.info("Checking node: "+n.getBrowseName().get().toParseableString());					
				String bName = n.getBrowseName().get().getName();
				if (bName.equalsIgnoreCase(HandshakeProtocol.STATE_SERVERSIDE_VAR_NAME))
					nodeIds.setStateVar(n.getNodeId().get());				
				else if (bName.equalsIgnoreCase(HandshakeProtocol.ServerMessageTypes.RequestInitiateHandover.toString()))
					nodeIds.setInitMethod(n.getNodeId().get());
				else if (bName.equalsIgnoreCase(HandshakeProtocol.ServerMessageTypes.RequestStartHandover.toString()))
					nodeIds.setStartMethod(n.getNodeId().get());
				else if (bName.equalsIgnoreCase("INIT_HANDOVER")) // FORTE IMPLEMENTATION
					nodeIds.setInitMethod(n.getNodeId().get());
				else if (bName.equalsIgnoreCase("START_HANDOVER")) // FORTE IMPLEMENTATION
					nodeIds.setStartMethod(n.getNodeId().get());
		}
		nodeIds.setCapabilityImplNode(info.getActorNode());
		return nodeIds;
	}
	
	private Optional<NodeId> getGrandParentForNodeIdViaBrowse(OpcUaClient client, NodeId nodeIdToSearchFor, NodeId currentNode, NodeId parent) {
		try {
			List<Node> nodes = client.getAddressSpace().browse(currentNode).get();
			Optional<NodeId> found = null;
			for (Node n : nodes) {
				NodeId nId = n.getNodeId().get();
				//logger.info("Checking node for capMatch: "+nId.toParseableString());
				if (nId.toParseableString().equalsIgnoreCase(nodeIdToSearchFor.toParseableString())) {
					return Optional.of(parent);
				} else {
					found = getGrandParentForNodeIdViaBrowse(client, nodeIdToSearchFor, nId, currentNode);
					if (found.isPresent()) return found;
				}
			} 
		} catch (Exception e) {
			e.printStackTrace();			
		}
		return Optional.empty();
	}

	@Override
	public void setStatusValue(String newStatus) {		
		if(status != null) {
			status.setValue(new DataValue(new Variant(newStatus)));
		}
	}
}
