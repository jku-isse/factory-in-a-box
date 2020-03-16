package fiab.opcua.hardwaremock.plotter;

import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import fiab.mes.mockactors.MockServerHandshakeActor;
import fiab.mes.opcua.OPCUACapabilitiesWellknownBrowsenames;
import fiab.mes.transport.handshake.HandshakeProtocol;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerSide;
import fiab.opcua.hardwaremock.OPCUABase;
import fiab.opcua.hardwaremock.StatePublisher;
import fiab.opcua.hardwaremock.iostation.methods.InitHandover;
import fiab.opcua.hardwaremock.iostation.methods.StartHandover;

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
		status = base.generateStringVariableNode(handshakeNode, path, HandshakeProtocol.STATE_SERVERSIDE_VAR_NAME, ServerSide.STOPPED);
		ActorRef localServer = context.actorOf(MockServerHandshakeActor.props(ttBaseActor, true, this));
		org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n1 = base.createPartialMethodNode(path, "INIT_HANDOVER", "Requests init");		
		base.addMethodNode(handshakeNode, n1, new InitHandover(n1, localServer)); 		
		org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n2 = base.createPartialMethodNode(path, "START_HANDOVER", "Requests start");		
		base.addMethodNode(handshakeNode, n2, new StartHandover(n2, localServer));
		// let parent Actor know, that there is a new endpoint		 			

		// add capabilities 
		UaFolderNode capabilitiesFolder = base.generateFolder(handshakeNode, path, new String( OPCUACapabilitiesWellknownBrowsenames.CAPABILITIES));
		path = path +"/"+OPCUACapabilitiesWellknownBrowsenames.CAPABILITIES;
		UaFolderNode capability1 = base.generateFolder(capabilitiesFolder, path,
				"CAPABILITY",  OPCUACapabilitiesWellknownBrowsenames.CAPABILITY);

		base.generateStringVariableNode(capability1, path+"/CAPABILITY",  OPCUACapabilitiesWellknownBrowsenames.TYPE,
				new String(HandshakeProtocol.HANDSHAKE_CAPABILITY_URI));
		base.generateStringVariableNode(capability1, path+"/CAPABILITY",  OPCUACapabilitiesWellknownBrowsenames.ID,
				"DefaultHandshakeFU");
		base.generateStringVariableNode(capability1, path+"/CAPABILITY",  OPCUACapabilitiesWellknownBrowsenames.ROLE,
				OPCUACapabilitiesWellknownBrowsenames.ROLE_VALUE_PROVIDED);

		return localServer;
	}
	

	@Override
	public void setStatusValue(String newStatus) {		
		if(status != null) {
			status.setValue(new DataValue(new Variant(newStatus)));
		}
	}
}
