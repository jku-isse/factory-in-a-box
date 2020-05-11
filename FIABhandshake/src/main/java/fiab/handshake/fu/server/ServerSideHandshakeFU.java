package fiab.handshake.fu.server;

import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import fiab.core.capabilities.StatePublisher;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.core.capabilities.handshake.IOStationCapability;
import fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames;
import fiab.core.capabilities.wiring.WiringInfo;
import fiab.handshake.actor.LocalEndpointStatus;
import fiab.handshake.actor.ServerSideHandshakeActor;
import fiab.handshake.fu.HandshakeFU;
import fiab.handshake.fu.server.methods.Complete;
import fiab.handshake.fu.server.methods.InitHandover;
import fiab.handshake.fu.server.methods.Reset;
import fiab.handshake.fu.server.methods.SetEmpty;
import fiab.handshake.fu.server.methods.SetLoaded;
import fiab.handshake.fu.server.methods.StartHandover;
import fiab.handshake.fu.server.methods.Stop;
import fiab.opcua.server.OPCUABase;

public class ServerSideHandshakeFU implements StatePublisher, HandshakeFU {

	private static final Logger logger = LoggerFactory.getLogger(ServerSideHandshakeFU.class);
	
	protected UaFolderNode rootNode;
	protected ActorRef parentActor;
	protected ActorContext context;
	protected String capInstId;
	protected String fuPrefix;
	protected OPCUABase base;
	protected boolean isProvided;
	protected org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode status = null;
	protected ActorRef localClient;
	protected boolean exposeInternalControl = true;
	
	public ServerSideHandshakeFU(OPCUABase base, UaFolderNode root, String fuPrefix, ActorRef parentActor, ActorContext context, String capInstId, boolean isProvided, boolean exposeInternalControl) {
		this.base = base;
		this.rootNode = root;
		this.parentActor = parentActor;
		this.context = context;
		this.capInstId = capInstId;
		this.fuPrefix = fuPrefix;
		this.isProvided = isProvided;
		this.exposeInternalControl = exposeInternalControl;
		setupActor();
		setupOPCUANodeSet();
	}
	
	@Override
	public ActorRef getFUActor() {
		return localClient;
	}
	
	protected void setupActor() {
		localClient = context.actorOf(ServerSideHandshakeActor.props(parentActor, true, this), capInstId);
	}
	
	private void setupOPCUANodeSet() {
		String path = fuPrefix + "/HANDSHAKE_FU_"+capInstId;
		UaFolderNode handshakeNode = base.generateFolder(rootNode, fuPrefix, "HANDSHAKE_FU_"+capInstId);	

		// add method/variables to opcua
		status = base.generateStringVariableNode(handshakeNode, path, IOStationCapability.OPCUA_STATE_SERVERSIDE_VAR_NAME, fiab.core.capabilities.handshake.HandshakeCapability.ServerSideStates.STOPPED);
		org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n1 = base.createPartialMethodNode(path, "INIT_HANDOVER", "Requests init");		
		base.addMethodNode(handshakeNode, n1, new InitHandover(n1, localClient)); 		
		org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n2 = base.createPartialMethodNode(path, "START_HANDOVER", "Requests start");		
		base.addMethodNode(handshakeNode, n2, new StartHandover(n2, localClient));

		if (exposeInternalControl) {
			// add reset and stop and complete methods, set loaded 
			org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n3 = base.createPartialMethodNode(path, IOStationCapability.ServerMessageTypes.Stop.toString(), "Request stop");		
			base.addMethodNode(handshakeNode, n3, new Stop(n3, localClient)); 
			org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n4 = base.createPartialMethodNode(path, IOStationCapability.ServerMessageTypes.Reset.toString(), "Request reset");		
			base.addMethodNode(handshakeNode, n4, new Reset(n4, localClient)); 
			org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n5 = base.createPartialMethodNode(path, IOStationCapability.ServerMessageTypes.Complete.toString(), "Request complete");		
			base.addMethodNode(handshakeNode, n5, new Complete(n5, localClient)); 
			org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n6 = base.createPartialMethodNode(path, HandshakeCapability.StateOverrideRequests.SetLoaded.toString(), "Request SetLoaded");		
			base.addMethodNode(handshakeNode, n6, new SetLoaded(n6, localClient)); 
			org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n7 = base.createPartialMethodNode(path, HandshakeCapability.StateOverrideRequests.SetEmpty.toString(), "Request SetEmpty");		
			base.addMethodNode(handshakeNode, n7, new SetEmpty(n7, localClient)); 
		}

		// let parent Actor know, that there is a new endpoint
		parentActor.tell(new LocalEndpointStatus.LocalServerEndpointStatus(localClient, isProvided, this.capInstId), ActorRef.noSender()); 			 

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
	
	@Override
	public void provideWiringInfo(WiringInfo info) throws Exception {
		
	}
	
	@Override
	public void setStatusValue(String newStatus) {		
		if(status != null) {
			status.setValue(new DataValue(new Variant(newStatus)));
		}
	}
}
