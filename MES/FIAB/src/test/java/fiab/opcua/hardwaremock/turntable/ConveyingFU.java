package fiab.opcua.hardwaremock.turntable;

import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import fiab.mes.machine.actor.WellknownMachinePropertyFields;
import fiab.mes.mockactors.transport.FUs.MockConveyorActor;
import fiab.mes.mockactors.transport.FUs.MockTurntableActor;
import fiab.mes.opcua.OPCUACapabilitiesWellknownBrowsenames;
import fiab.opcua.hardwaremock.OPCUABase;
import fiab.opcua.hardwaremock.StatePublisher;
import fiab.opcua.hardwaremock.turntable.methods.ConveyingLoad;
import fiab.opcua.hardwaremock.turntable.methods.ConveyingReset;
import fiab.opcua.hardwaremock.turntable.methods.ConveyingStop;
import fiab.opcua.hardwaremock.turntable.methods.ConveyingUnload;
import fiab.opcua.hardwaremock.turntable.methods.TurningRequest;
import fiab.opcua.hardwaremock.turntable.methods.TurningReset;
import fiab.opcua.hardwaremock.turntable.methods.TurningStop;
import stateMachines.conveyor.ConveyorStates;
import stateMachines.conveyor.ConveyorTriggers;
import stateMachines.turning.TurningStates;
import stateMachines.turning.TurningTriggers;

public class ConveyingFU implements StatePublisher{

	private static final Logger logger = LoggerFactory.getLogger(ConveyingFU.class);

	UaFolderNode rootNode;
	ActorContext context;
	String fuPrefix;
	OPCUABase base;

	private org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode status = null;


	public ConveyingFU(OPCUABase base, UaFolderNode root, String fuPrefix, ActorContext context) {
		this.base = base;
		this.rootNode = root;

		this.context = context;
		this.fuPrefix = fuPrefix;

		setupOPCUANodeSet();
	}


	private void setupOPCUANodeSet() {
		String path = fuPrefix + "/CONVEYING_FU";
		UaFolderNode handshakeNode = base.generateFolder(rootNode, fuPrefix, "CONVEYING_FU");	

		ActorRef turningActor = context.actorOf(MockConveyorActor.props(null, this), "TT1-ConveyingFU");

		status = base.generateStringVariableNode(handshakeNode, path, WellknownMachinePropertyFields.STATE_VAR_NAME, ConveyorStates.STOPPED);

		org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n1 = base.createPartialMethodNode(path, ConveyorTriggers.RESET.toString(), "Requests reset");		
		base.addMethodNode(handshakeNode, n1, new ConveyingReset(n1, turningActor)); 		
		org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n2 = base.createPartialMethodNode(path, ConveyorTriggers.STOP.toString(), "Requests stop");		
		base.addMethodNode(handshakeNode, n2, new ConveyingStop(n2, turningActor));
		org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n3 = base.createPartialMethodNode(path, ConveyorTriggers.LOAD.toString(), "Requests load");		
		base.addMethodNode(handshakeNode, n3, new ConveyingLoad(n3, turningActor));
		org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n4 = base.createPartialMethodNode(path, ConveyorTriggers.UNLOAD.toString(), "Requests unload");		
		base.addMethodNode(handshakeNode, n4, new ConveyingUnload(n4, turningActor));

		// add capabilities 
		UaFolderNode capabilitiesFolder = base.generateFolder(handshakeNode, path, new String( OPCUACapabilitiesWellknownBrowsenames.CAPABILITIES));
		path = path +"/"+OPCUACapabilitiesWellknownBrowsenames.CAPABILITIES;
		UaFolderNode capability1 = base.generateFolder(capabilitiesFolder, path,
				"CAPABILITY",  OPCUACapabilitiesWellknownBrowsenames.CAPABILITY);

		base.generateStringVariableNode(capability1, path+"/CAPABILITY",  OPCUACapabilitiesWellknownBrowsenames.TYPE,
				new String("http://factory-in-a-box.fiab/capabilities/transport/conveying"));
		base.generateStringVariableNode(capability1, path+"/CAPABILITY",  OPCUACapabilitiesWellknownBrowsenames.ID,
				new String("DefaultConveyingCapability"));
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
