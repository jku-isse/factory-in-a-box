package fiab.opcua.hardwaremock.turntable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.nodes.Node;
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
import fiab.core.capabilities.handshake.IOStationCapability;
import fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames;
import fiab.mes.mockactors.MockClientHandshakeActor;
import fiab.mes.mockactors.MockServerHandshakeActor;
import fiab.mes.mockactors.MockServerHandshakeActor.StateOverrideRequests;
import fiab.mes.mockactors.transport.LocalEndpointStatus;
import fiab.mes.opcua.OPCUAUtils;
import fiab.opcua.hardwaremock.OPCUABase;
import fiab.opcua.hardwaremock.StatePublisher;
import fiab.opcua.hardwaremock.clienthandshake.OPCUAClientHandshakeActorWrapper;
import fiab.opcua.hardwaremock.clienthandshake.OPCUAClientHandshakeActorWrapper.ServerHandshakeNodeIds;
import fiab.opcua.hardwaremock.iostation.methods.InitHandover;
import fiab.opcua.hardwaremock.iostation.methods.StartHandover;
import fiab.opcua.hardwaremock.serverhandshake.methods.Complete;
import fiab.opcua.hardwaremock.serverhandshake.methods.Reset;
import fiab.opcua.hardwaremock.serverhandshake.methods.SetEmpty;
import fiab.opcua.hardwaremock.serverhandshake.methods.SetLoaded;
import fiab.opcua.hardwaremock.serverhandshake.methods.Stop;
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
	private boolean enableCoordinatorActor = true;
	
	public HandshakeFU(OPCUABase base, UaFolderNode root, String fuPrefix, ActorRef ttBaseActor, ActorContext context, String capInstId, boolean isProvided, boolean enableCoordinatorActor) {
		this.base = base;
		this.rootNode = root;
		this.ttBaseActor = ttBaseActor;
		this.context = context;
		this.capInstId = capInstId;
		this.fuPrefix = fuPrefix;
		this.isProvided = isProvided;
		this.enableCoordinatorActor = enableCoordinatorActor;
		setupOPCUANodeSet();
	}
	
	
	private void setupOPCUANodeSet() {
		String path = fuPrefix + "/HANDSHAKE_FU_"+capInstId;
		UaFolderNode handshakeNode = base.generateFolder(rootNode, fuPrefix, "HANDSHAKE_FU_"+capInstId);	
		
		// add method/variables to opcua
		if (isProvided) { // add all the methods available here, actor is the serverhandshake		
			status = base.generateStringVariableNode(handshakeNode, path, IOStationCapability.STATE_SERVERSIDE_VAR_NAME, fiab.core.capabilities.handshake.HandshakeCapability.ServerSide.STOPPED);
			localClient = context.actorOf(MockServerHandshakeActor.props(ttBaseActor, true, this));
			org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n1 = base.createPartialMethodNode(path, "INIT_HANDOVER", "Requests init");		
			base.addMethodNode(handshakeNode, n1, new InitHandover(n1, localClient)); 		
			org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n2 = base.createPartialMethodNode(path, "START_HANDOVER", "Requests start");		
			base.addMethodNode(handshakeNode, n2, new StartHandover(n2, localClient));
			
			if (!enableCoordinatorActor) {
				// add reset and stop and complete methods, set loaded 
				org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n3 = base.createPartialMethodNode(path, IOStationCapability.ServerMessageTypes.Stop.toString(), "Request stop");		
				base.addMethodNode(handshakeNode, n3, new Stop(n3, localClient)); 
				org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n4 = base.createPartialMethodNode(path, IOStationCapability.ServerMessageTypes.Reset.toString(), "Request reset");		
				base.addMethodNode(handshakeNode, n4, new Reset(n4, localClient)); 
				org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n5 = base.createPartialMethodNode(path, IOStationCapability.ServerMessageTypes.Complete.toString(), "Request complete");		
				base.addMethodNode(handshakeNode, n5, new Complete(n5, localClient)); 
				org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n6 = base.createPartialMethodNode(path, StateOverrideRequests.SetLoaded.toString(), "Request SetLoaded");		
				base.addMethodNode(handshakeNode, n6, new SetLoaded(n6, localClient)); 
				org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n7 = base.createPartialMethodNode(path, StateOverrideRequests.SetEmpty.toString(), "Request SetEmpty");		
				base.addMethodNode(handshakeNode, n7, new SetEmpty(n7, localClient)); 
			}
			
			// let parent Actor know, that there is a new endpoint
			ttBaseActor.tell(new LocalEndpointStatus.LocalServerEndpointStatus(localClient, isProvided, this.capInstId), ActorRef.noSender()); 			 
		} else {
			status = base.generateStringVariableNode(handshakeNode, path, IOStationCapability.STATE_CLIENTSIDE_VAR_NAME, fiab.core.capabilities.handshake.HandshakeCapability.ClientSide.STOPPED); 
			opcuaWrapper = context.actorOf(OPCUAClientHandshakeActorWrapper.props(), capInstId+"_OPCUAWrapper");
			localClient = context.actorOf(MockClientHandshakeActor.props(ttBaseActor, opcuaWrapper, this), capInstId);
			opcuaWrapper.tell(localClient, ActorRef.noSender());
			ttBaseActor.tell(new LocalEndpointStatus.LocalClientEndpointStatus(localClient, isProvided, this.capInstId), ActorRef.noSender()); 
			
			if (!enableCoordinatorActor) {
				// add reset, start, and stop and complete method
				org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n3 = base.createPartialMethodNode(path, IOStationCapability.ClientMessageTypes.Stop.toString(), "Request stop");		
				base.addMethodNode(handshakeNode, n3, new fiab.opcua.hardwaremock.clienthandshake.methods.Stop(n3, localClient)); 
				org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n4 = base.createPartialMethodNode(path, IOStationCapability.ClientMessageTypes.Reset.toString(), "Request reset");		
				base.addMethodNode(handshakeNode, n4, new fiab.opcua.hardwaremock.clienthandshake.methods.Reset(n4, localClient)); 
				org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n5 = base.createPartialMethodNode(path, IOStationCapability.ClientMessageTypes.Complete.toString(), "Request complete");		
				base.addMethodNode(handshakeNode, n5, new fiab.opcua.hardwaremock.clienthandshake.methods.Complete(n5, localClient)); 
				org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n6 = base.createPartialMethodNode(path, IOStationCapability.ClientMessageTypes.Start.toString(), "Start complete");		
				base.addMethodNode(handshakeNode, n6, new fiab.opcua.hardwaremock.clienthandshake.methods.Start(n6, localClient)); 
			}
		}
		
		// add capabilities 
		UaFolderNode capabilitiesFolder = base.generateFolder(handshakeNode, path, new String( OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITIES));
		path = path +"/"+OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITIES;
		UaFolderNode capability1 = base.generateFolder(capabilitiesFolder, path,
				"CAPABILITY",  OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITY);
		
		base.generateStringVariableNode(capability1, path+"/CAPABILITY",  OPCUACapabilitiesAndWiringInfoBrowsenames.TYPE,
				new String(IOStationCapability.HANDSHAKE_CAPABILITY_URI));
		base.generateStringVariableNode(capability1, path+"/CAPABILITY",  OPCUACapabilitiesAndWiringInfoBrowsenames.ID,
				new String(capInstId));
		base.generateStringVariableNode(capability1, path+"/CAPABILITY",  OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE,
				new String(isProvided ? OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE_VALUE_PROVIDED : OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE_VALUE_REQUIRED));
		
		
		
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
					CapabilityImplInfo cii = new CapabilityImplInfo(info.getRemoteEndpointURL(), optActorCapImplNodeId.get(), optRemoteNodeId.get(), IOStationCapability.HANDSHAKE_CAPABILITY_URI);
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
				if (bName.equalsIgnoreCase(IOStationCapability.STATE_SERVERSIDE_VAR_NAME))
					nodeIds.setStateVar(n.getNodeId().get());				
				else if (bName.equalsIgnoreCase(IOStationCapability.ServerMessageTypes.RequestInitiateHandover.toString()))
					nodeIds.setInitMethod(n.getNodeId().get());
				else if (bName.equalsIgnoreCase(IOStationCapability.ServerMessageTypes.RequestStartHandover.toString()))
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
