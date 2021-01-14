package fiab.turntable.turning.fu.opcua;

import config.HardwareInfo;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import main.java.fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import main.java.fiab.core.capabilities.StatePublisher;
import main.java.fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames;
import fiab.opcua.server.OPCUABase;
import fiab.turntable.actor.IntraMachineEventBus;
import fiab.turntable.turning.statemachine.TurningStates;
import fiab.turntable.turning.statemachine.TurningTriggers;
import fiab.turntable.turning.TurntableActor;
import fiab.turntable.turning.fu.opcua.methods.TurningRequest;
import fiab.turntable.turning.fu.opcua.methods.TurningReset;
import fiab.turntable.turning.fu.opcua.methods.TurningStop;

public class TurningFU implements StatePublisher {

    private static final Logger logger = LoggerFactory.getLogger(TurningFU.class);

    UaFolderNode rootNode;
    ActorContext context;
    String fuPrefix;
    OPCUABase base;
    ActorRef turningActor;

    private org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode status = null;


    public TurningFU(OPCUABase base, UaFolderNode root, String fuPrefix, ActorContext context, boolean exposeInternalControl, IntraMachineEventBus intraEventBus, HardwareInfo hardwareInfo) {
        this.base = base;
        this.rootNode = root;

        this.context = context;
        this.fuPrefix = fuPrefix;

        setupOPCUANodeSet(exposeInternalControl, intraEventBus, hardwareInfo);
    }


    private void setupOPCUANodeSet(boolean exposeInternalControl, IntraMachineEventBus intraEventBus, HardwareInfo hardwareInfo) {
        String path = fuPrefix + "/TURNING_FU";
        UaFolderNode turningFolder = base.generateFolder(rootNode, fuPrefix, "TURNING_FU");
        String machineName = fuPrefix.split("/")[0];    //First part of prefix is machine name
        turningActor = context.actorOf(TurntableActor.props(intraEventBus, this, hardwareInfo), "TurningFU");

        status = base.generateStringVariableNode(turningFolder, path, OPCUABasicMachineBrowsenames.STATE_VAR_NAME, TurningStates.STOPPED);

        if (exposeInternalControl) {
            org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n1 = base.createPartialMethodNode(path, TurningTriggers.RESET.toString(), "Requests reset");
            base.addMethodNode(turningFolder, n1, new TurningReset(n1, turningActor));
            org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n2 = base.createPartialMethodNode(path, TurningTriggers.STOP.toString(), "Requests stop");
            base.addMethodNode(turningFolder, n2, new TurningStop(n2, turningActor));
            org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode n3 = base.createPartialMethodNode(path, "RequestTurn", "Requests turning");
            base.addMethodNode(turningFolder, n3, new TurningRequest(n3, turningActor));
        }

        // add capabilities
        UaFolderNode capabilitiesFolder = base.generateFolder(turningFolder, path, new String(OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITIES));
        path = path + "/" + OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITIES;
        UaFolderNode capability1 = base.generateFolder(capabilitiesFolder, path,
                "CAPABILITY", OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITY);

        base.generateStringVariableNode(capability1, path + "/CAPABILITY", OPCUACapabilitiesAndWiringInfoBrowsenames.TYPE,
                new String("http://factory-in-a-box.fiab/capabilities/transport/turning"));
        base.generateStringVariableNode(capability1, path + "/CAPABILITY", OPCUACapabilitiesAndWiringInfoBrowsenames.ID,
                new String("DefaultTurningCapability"));
        base.generateStringVariableNode(capability1, path + "/CAPABILITY", OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE,
                new String(OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE_VALUE_PROVIDED));

        addOpcUaHardwareRefs(turningFolder, path, machineName);
    }

    private void addOpcUaHardwareRefs(UaFolderNode turningFolder, String path, String machineName) {
        UaFolderNode hardwareFolder = base.generateFolder(turningFolder, path, "Hardware");
        base.generateStringVariableNode(hardwareFolder, path + "/Hardware/TurningMotor", "TurningMotor", machineName + "/Hardware/Elements/MotorD");
        base.generateStringVariableNode(hardwareFolder, path + "/Hardware/TurningMotor", "HomingSensor", machineName + "/Hardware/Elements/Sensor4");
    }


    public ActorRef getActor() {
        return turningActor;
    }

    @Override
    public void setStatusValue(String newStatus) {
        if (status != null) {
            status.setValue(new DataValue(new Variant(newStatus)));
        }
    }
}
