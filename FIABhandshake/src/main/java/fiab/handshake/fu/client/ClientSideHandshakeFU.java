package fiab.handshake.fu.client;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.handshake.fu.HandshakeFU;

import org.eclipse.milo.opcua.sdk.client.ModifiedOpcUaClient;
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
import fiab.core.capabilities.StatePublisher;
import fiab.core.capabilities.handshake.IOStationCapability;
import fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames;
import fiab.core.capabilities.wiring.WiringInfo;
import fiab.handshake.actor.ClientHandshakeActor;
import fiab.handshake.actor.LocalEndpointStatus;
import fiab.opcua.CapabilityImplInfo;
//import fiab.handshake.fu.HandshakeFU;
import fiab.handshake.fu.client.methods.Complete;
import fiab.handshake.fu.client.methods.Reset;
import fiab.handshake.fu.client.methods.Start;
import fiab.handshake.fu.client.methods.Stop;
import fiab.opcua.WiringExposingUtils;
import fiab.opcua.client.OPCUAClientFactory;
import fiab.opcua.server.OPCUABase;
import fiab.opcua.wiring.WiringException;
import fiab.opcua.wiring.WiringNodes;
import fiab.opcua.wiring.WiringRequest;
import fiab.opcua.wiring.WiringUpdateInterface;
import zipkin2.reporter.AsyncReporter;

public class ClientSideHandshakeFU implements StatePublisher, HandshakeFU, WiringUpdateInterface{

	private static final Logger logger = LoggerFactory.getLogger(ClientSideHandshakeFU.class);
	
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
	OpcUaClient client;
	boolean isProvided;
	private org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode status = null;
	private ActorRef opcuaWrapper;
	private ActorRef localClient;
	private boolean exposeInternalControl = true;
	private WiringNodes wiringNodes;
	private AsyncReporter<zipkin2.Span> reporter;
	
	public ClientSideHandshakeFU(OPCUABase base, UaFolderNode root, String fuPrefix, ActorRef ttBaseActor, ActorContext context, String capInstId, boolean isProvided, boolean exposeInternalControl) {
		this.base = base;
		this.rootNode = root;
		this.ttBaseActor = ttBaseActor;
		this.context = context;
		this.capInstId = capInstId;
		this.fuPrefix = fuPrefix;
		this.isProvided = isProvided;
		this.exposeInternalControl = exposeInternalControl;
		setupOPCUANodeSet();
	}
	
	public ClientSideHandshakeFU(OPCUABase base, UaFolderNode root, String fuPrefix, ActorRef ttBaseActor, ActorContext context, String capInstId, boolean isProvided, boolean exposeInternalControl, AsyncReporter<zipkin2.Span> reporter) {
		this.base = base;
		this.rootNode = root;
		this.ttBaseActor = ttBaseActor;
		this.context = context;
		this.capInstId = capInstId;
		this.fuPrefix = fuPrefix;
		this.isProvided = isProvided;
		this.exposeInternalControl = exposeInternalControl;
		this.reporter = reporter;
		setupOPCUANodeSet();
	}
	
	private void setupOPCUANodeSet() {
		String path = fuPrefix + "/HANDSHAKE_FU_"+capInstId;
		UaFolderNode handshakeNode = base.generateFolder(rootNode, fuPrefix, "HANDSHAKE_FU_"+capInstId);	
		
		
			status = base.generateStringVariableNode(handshakeNode, path, IOStationCapability.OPCUA_STATE_CLIENTSIDE_VAR_NAME, HandshakeCapability.ClientSideStates.STOPPED);
			opcuaWrapper = context.actorOf(OPCUAClientHandshakeActorWrapper.props(), capInstId+"_OPCUAWrapper");
			localClient = context.actorOf(ClientHandshakeActor.props(ttBaseActor, opcuaWrapper, this), capInstId);
			opcuaWrapper.tell(localClient, ActorRef.noSender());
			ttBaseActor.tell(new LocalEndpointStatus.LocalClientEndpointStatus(localClient, isProvided, this.capInstId), ActorRef.noSender()); 
			
			if (exposeInternalControl) {
				// add reset, start, and stop and complete method
				org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n3 = base.createPartialMethodNode(path, IOStationCapability.ClientMessageTypes.Stop.toString(), "Request stop");		
				base.addMethodNode(handshakeNode, n3, new Stop(n3, localClient)); 
				org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n4 = base.createPartialMethodNode(path, IOStationCapability.ClientMessageTypes.Reset.toString(), "Request reset");		
				base.addMethodNode(handshakeNode, n4, new Reset(n4, localClient)); 
				org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n5 = base.createPartialMethodNode(path, IOStationCapability.ClientMessageTypes.Complete.toString(), "Request complete");		
				base.addMethodNode(handshakeNode, n5, new Complete(n5, localClient)); 
				org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n6 = base.createPartialMethodNode(path, IOStationCapability.ClientMessageTypes.Start.toString(), "Request start");		
				base.addMethodNode(handshakeNode, n6, new Start(n6, localClient)); 
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
		String wiringsFolderPath = path + "/CAPABILITY/WIRINGS";
        UaFolderNode wiringsFolder = base.generateFolder(capability1,wiringsFolderPath, "WIRINGS", "WIRINGS" );
        
		// add wiring method
		wiringNodes = WiringExposingUtils.createWiringInfoFolder(base, handshakeNode, path);
		org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode nWire = base.createPartialMethodNode(path, OPCUACapabilitiesAndWiringInfoBrowsenames.OPCUA_WIRING_REQUEST, "Request stop");		
		base.addMethodNode(handshakeNode, nWire, new WiringRequest(nWire, this)); 
	}
	
	@Override
	public void provideWiringInfo(WiringInfo info) throws WiringException {

		// process wiring info --> create new opcua client, and recreate wrapper actor

		logger.info("Applying Wiring Info for required Capability: "+capInstId);	
		if (client != null) {
			try {
				logger.info("Trying to disconnect remote client: " + client.toString());
				WiringExposingUtils.updateWiring(wiringNodes, "", "", "", "");
				client.disconnect().get();
				//Clearing the wiring Information
				logger.info("Disconnected remote Client.  " + client.toString());
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				client = null;
			}
		}
		if (info.getRemoteEndpointURL().isEmpty()) // unset of wiring, thus done here
			return;
		
		//TODO: switch between tracing and non tracing client
		try {
			client = new OPCUAClientFactory().createTracingClient(info.getRemoteEndpointURL(), reporter);
			client.connect().get();

		} catch (Exception e) {
			logger.warn(e.getMessage());
			throw new WiringException("Could not connect to remote endpoint");
		}
		logger.info("OPCUA Client connected for FU: "+this.capInstId);
		Optional<NodeId> optRemoteNodeId = NodeId.parseSafe(info.getRemoteNodeId());
		if (optRemoteNodeId.isPresent()) {					
			logger.info("Searching for Grandparent Node for Capability: "+optRemoteNodeId.get().toParseableString());
			Optional<NodeId> optActorCapImplNodeId = getGrandParentForNodeIdViaBrowse(client, optRemoteNodeId.get(), Identifiers.RootFolder, null);
			if (optActorCapImplNodeId.isPresent()) {
				fiab.opcua.CapabilityImplInfo cii = new fiab.opcua.CapabilityImplInfo(info.getRemoteEndpointURL(), optActorCapImplNodeId.get(), optRemoteNodeId.get(), IOStationCapability.HANDSHAKE_CAPABILITY_URI);
				cii.setClient(client);
				ServerHandshakeNodeIds nodeIds;
				try {
					nodeIds = retrieveNodeIds(cii);
				} catch (InterruptedException | ExecutionException e) {
					logger.warn(e.getMessage());
					throw new WiringException("Could not browse/retrieve capability nodes from remote endpoint");
				}
				if (!nodeIds.isComplete()) {
					String msg = "OPCUA Client Endpoints incompletely resolved for FU: "+this.capInstId + " at "+optActorCapImplNodeId.get().toParseableString();
					logger.warn(msg);
					throw new WiringException(msg);
				} else {
					nodeIds.setClient(client);
					logger.info("OPCUA Client Endpoints resolved for FU: "+this.capInstId + " at "+optActorCapImplNodeId.get().toParseableString());
					opcuaWrapper.tell(nodeIds, ActorRef.noSender());
					WiringExposingUtils.updateWiring(wiringNodes, info.getRemoteCapabilityId(), info.getRemoteEndpointURL(), info.getRemoteNodeId(), info.getRemoteRole());
				}
			} else {
				String msg = "Could not resolve actor cap impl for nodeId:" +info.getRemoteNodeId();
				logger.warn(msg);
				throw new WiringException(msg);
			}
		} else {
			String msg = "Could not resolve nodeId:" +info.getRemoteNodeId();
			logger.warn(msg);
			throw new WiringException(msg);
		}
	}

	
	private ServerHandshakeNodeIds retrieveNodeIds(CapabilityImplInfo info) throws InterruptedException, ExecutionException {
		List<Node> nodes = info.getClient().getAddressSpace().browse(info.getActorNode()).get();		
		// we assume unique node names and method names within this hierarchy level (thus no two capabilities with overlapping browse names)
		ServerHandshakeNodeIds nodeIds = new ServerHandshakeNodeIds();
		for (Node n : nodes) {
			//logger.info("Checking node: "+n.getBrowseName().get().toParseableString());					
				String bName = n.getBrowseName().get().getName();
				if (bName.equalsIgnoreCase(IOStationCapability.OPCUA_STATE_SERVERSIDE_VAR_NAME))
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


	@Override
	public ActorRef getFUActor() {
		return localClient;
	}
}
