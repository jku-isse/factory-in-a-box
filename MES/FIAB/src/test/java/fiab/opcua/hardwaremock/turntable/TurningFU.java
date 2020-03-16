package fiab.opcua.hardwaremock.turntable;

import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import fiab.mes.machine.actor.WellknownMachinePropertyFields;
import fiab.mes.mockactors.transport.FUs.MockTurntableActor;
import fiab.mes.opcua.OPCUACapabilitiesWellknownBrowsenames;
import fiab.opcua.hardwaremock.OPCUABase;
import fiab.opcua.hardwaremock.StatePublisher;
import fiab.opcua.hardwaremock.turntable.methods.TurningRequest;
import fiab.opcua.hardwaremock.turntable.methods.TurningReset;
import fiab.opcua.hardwaremock.turntable.methods.TurningStop;
import stateMachines.turning.TurningStates;
import stateMachines.turning.TurningTriggers;

public class TurningFU implements StatePublisher{

	private static final Logger logger = LoggerFactory.getLogger(TurningFU.class);

	UaFolderNode rootNode;
	ActorContext context;
	String fuPrefix;
	OPCUABase base;

	private org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode status = null;


	public TurningFU(OPCUABase base, UaFolderNode root, String fuPrefix, ActorContext context) {
		this.base = base;
		this.rootNode = root;

		this.context = context;
		this.fuPrefix = fuPrefix;

		setupOPCUANodeSet();
	}


	private void setupOPCUANodeSet() {
		String path = fuPrefix + "/TURNING_FU";
		UaFolderNode handshakeNode = base.generateFolder(rootNode, fuPrefix, "TURNING_FU");	

		ActorRef turningActor = context.actorOf(MockTurntableActor.props(null, this), "TT1-TurningFU");

		status = base.generateStringVariableNode(handshakeNode, path, WellknownMachinePropertyFields.STATE_VAR_NAME, TurningStates.STOPPED);

		org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n1 = base.createPartialMethodNode(path, TurningTriggers.RESET.toString(), "Requests reset");		
		base.addMethodNode(handshakeNode, n1, new TurningReset(n1, turningActor)); 		
		org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n2 = base.createPartialMethodNode(path, TurningTriggers.STOP.toString(), "Requests stop");		
		base.addMethodNode(handshakeNode, n2, new TurningStop(n2, turningActor));
		org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n3 = base.createPartialMethodNode(path, "RequestTurn", "Requests turning");		
		base.addMethodNode(handshakeNode, n3, new TurningRequest(n3, turningActor));


		// add capabilities 
		UaFolderNode capabilitiesFolder = base.generateFolder(handshakeNode, path, new String( OPCUACapabilitiesWellknownBrowsenames.CAPABILITIES));
		path = path +"/"+OPCUACapabilitiesWellknownBrowsenames.CAPABILITIES;
		UaFolderNode capability1 = base.generateFolder(capabilitiesFolder, path,
				"CAPABILITY",  OPCUACapabilitiesWellknownBrowsenames.CAPABILITY);

		base.generateStringVariableNode(capability1, path+"/CAPABILITY",  OPCUACapabilitiesWellknownBrowsenames.TYPE,
				new String("http://factory-in-a-box.fiab/capabilities/transport/turning"));
		base.generateStringVariableNode(capability1, path+"/CAPABILITY",  OPCUACapabilitiesWellknownBrowsenames.ID,
				new String("DefaultTurningCapability"));
		base.generateStringVariableNode(capability1, path+"/CAPABILITY",  OPCUACapabilitiesWellknownBrowsenames.ROLE,
				new String(OPCUACapabilitiesWellknownBrowsenames.ROLE_VALUE_PROVIDED));
	}

	@Override
	public void setStatusValue(String newStatus) {		
		if(status != null) {
			status.setValue(new DataValue(new Variant(newStatus)));
		}
	}
}
