package fiab.opcua.hardwaremock.plotter;

import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import fiab.core.capabilities.StatePublisher;
import fiab.core.capabilities.handshake.IOStationCapability;
import fiab.core.capabilities.handshake.HandshakeCapability.ServerSideStates;
import fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames;
import fiab.handshake.actor.ServerSideHandshakeActor;
import fiab.handshake.fu.server.methods.InitHandover;
import fiab.handshake.fu.server.methods.StartHandover;
import fiab.opcua.server.OPCUABase;

public class HandshakeFU implements StatePublisher{

	private static final Logger logger = LoggerFactory.getLogger(HandshakeFU.class);
	
	// add wiring endpoint 
	// load wiring from file 
	// obtain wiring from opc-ua call
	// update wiring info in opc-ua model
	
//	UaFolderNode rootNode;
//	String capInstId;
//	String fuPrefix;
//	OPCUABase base;
//	boolean isProvided;
	private org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode status = null;	
	
	public HandshakeFU() {
	
	}
		
	public ActorRef setupOPCUANodeSet(ActorRef ttBaseActor, OPCUABase base, UaFolderNode rootNode, String fuPrefix, ActorContext context) {
		String path = fuPrefix + "/HANDSHAKE_FU";
		UaFolderNode handshakeNode = base.generateFolder(rootNode, fuPrefix, "HANDSHAKE_FU");					
		status = base.generateStringVariableNode(handshakeNode, path, IOStationCapability.OPCUA_STATE_SERVERSIDE_VAR_NAME, ServerSideStates.STOPPED);
		ActorRef localServer = context.actorOf(ServerSideHandshakeActor.props(ttBaseActor, true, this));
		org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n1 = base.createPartialMethodNode(path, "INIT_HANDOVER", "Requests init");		
		base.addMethodNode(handshakeNode, n1, new InitHandover(n1, localServer)); 		
		org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n2 = base.createPartialMethodNode(path, "START_HANDOVER", "Requests start");		
		base.addMethodNode(handshakeNode, n2, new StartHandover(n2, localServer));
		// let parent Actor know, that there is a new endpoint		 			

		// add capabilities 
		UaFolderNode capabilitiesFolder = base.generateFolder(handshakeNode, path, new String( OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITIES));
		path = path +"/"+OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITIES;
		UaFolderNode capability1 = base.generateFolder(capabilitiesFolder, path,
				"CAPABILITY",  OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITY);

		base.generateStringVariableNode(capability1, path+"/CAPABILITY",  OPCUACapabilitiesAndWiringInfoBrowsenames.TYPE,
				new String(IOStationCapability.HANDSHAKE_CAPABILITY_URI));
		base.generateStringVariableNode(capability1, path+"/CAPABILITY",  OPCUACapabilitiesAndWiringInfoBrowsenames.ID,
				"DefaultHandshakeFU");
		base.generateStringVariableNode(capability1, path+"/CAPABILITY",  OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE,
				OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE_VALUE_PROVIDED);

		return localServer;
	}
	

	@Override
	public void setStatusValue(String newStatus) {		
		if(status != null) {
			status.setValue(new DataValue(new Variant(newStatus)));
		}
	}
}
