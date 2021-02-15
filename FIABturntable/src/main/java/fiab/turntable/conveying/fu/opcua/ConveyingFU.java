package fiab.turntable.conveying.fu.opcua;

import config.HardwareInfo;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.StatePublisher;
import fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames;
import fiab.opcua.server.OPCUABase;
import fiab.turntable.actor.IntraMachineEventBus;
import fiab.turntable.conveying.ConveyorActor;
import fiab.turntable.conveying.statemachine.ConveyorStates;
import fiab.turntable.conveying.statemachine.ConveyorTriggers;
import fiab.turntable.conveying.fu.opcua.methods.ConveyingLoad;
import fiab.turntable.conveying.fu.opcua.methods.ConveyingReset;
import fiab.turntable.conveying.fu.opcua.methods.ConveyingStop;
import fiab.turntable.conveying.fu.opcua.methods.ConveyingUnload;

public class ConveyingFU implements StatePublisher {

    private static final Logger logger = LoggerFactory.getLogger(ConveyingFU.class);

    UaFolderNode rootNode;
    ActorContext context;
    String fuPrefix;
    OPCUABase base;
    ActorRef conveyingActor;

    private org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode status = null;


    public ConveyingFU(OPCUABase base, UaFolderNode root, String fuPrefix, ActorContext context,
                       boolean exposeInternalControl, IntraMachineEventBus intraEventBus, HardwareInfo hardwareInfo) {
        this.base = base;
        this.rootNode = root;

        this.context = context;
        this.fuPrefix = fuPrefix;

        setupOPCUANodeSet(exposeInternalControl, intraEventBus, hardwareInfo);
    }


    private void setupOPCUANodeSet(boolean exposeInternalControl, IntraMachineEventBus intraEventBus, HardwareInfo hardwareInfo) {
        String path = fuPrefix + "/CONVEYING_FU";
        UaFolderNode conveyorFolder = base.generateFolder(rootNode, fuPrefix, "CONVEYING_FU");
        String machineName = fuPrefix.split("/")[0];
        conveyingActor = context.actorOf(ConveyorActor.props(intraEventBus, this, hardwareInfo), "TT-ConveyingFU");

        status = base.generateStringVariableNode(conveyorFolder, path, OPCUABasicMachineBrowsenames.STATE_VAR_NAME, ConveyorStates.STOPPED);

        if (exposeInternalControl) {
            org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n1 = base.createPartialMethodNode(path, ConveyorTriggers.RESET.toString(), "Requests reset");
            base.addMethodNode(conveyorFolder, n1, new ConveyingReset(n1, conveyingActor));
            org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n2 = base.createPartialMethodNode(path, ConveyorTriggers.STOP.toString(), "Requests stop");
            base.addMethodNode(conveyorFolder, n2, new ConveyingStop(n2, conveyingActor));
            org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n3 = base.createPartialMethodNode(path, ConveyorTriggers.LOAD.toString(), "Requests load");
            base.addMethodNode(conveyorFolder, n3, new ConveyingLoad(n3, conveyingActor));
            org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n4 = base.createPartialMethodNode(path, ConveyorTriggers.UNLOAD.toString(), "Requests unload");
            base.addMethodNode(conveyorFolder, n4, new ConveyingUnload(n4, conveyingActor));
        }
        // add capabilities
        UaFolderNode capabilitiesFolder = base.generateFolder(conveyorFolder, path, new String(OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITIES));
        path = path + "/" + OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITIES;
        UaFolderNode capability1 = base.generateFolder(capabilitiesFolder, path,
                "CAPABILITY", OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITY);

        base.generateStringVariableNode(capability1, path + "/CAPABILITY", OPCUACapabilitiesAndWiringInfoBrowsenames.TYPE,
                new String("http://factory-in-a-box.fiab/capabilities/transport/conveying"));
        base.generateStringVariableNode(capability1, path + "/CAPABILITY", OPCUACapabilitiesAndWiringInfoBrowsenames.ID,
                new String("DefaultConveyingCapability"));
        base.generateStringVariableNode(capability1, path + "/CAPABILITY", OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE,
                new String(OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE_VALUE_PROVIDED));

        addOpcUaHardwareRefs(conveyorFolder, path, machineName);
    }

    private void addOpcUaHardwareRefs(UaFolderNode conveyorFolder, String path, String machineName) {
        UaFolderNode hardwareFolder = base.generateFolder(conveyorFolder, path, "Hardware");
        base.generateStringVariableNode(hardwareFolder, path + "/Hardware/ConveyorMotor", "ConveyorMotor", machineName + "/Hardware/Elements/MotorA");
        base.generateStringVariableNode(hardwareFolder, path + "/Hardware/LoadingSensor", "LoadingSensor", machineName + "/Hardware/Elements/Sensor2");
        base.generateStringVariableNode(hardwareFolder, path + "/Hardware/UnloadingSensor", "UnloadingSensor", machineName + "/Hardware/Elements/Sensor3");
    }

    public ActorRef getActor() {
        return conveyingActor;
    }

    @Override
    public void setStatusValue(String newStatus) {
        if (status != null) {
            status.setValue(new DataValue(new Variant(newStatus)));
        }
    }
}
