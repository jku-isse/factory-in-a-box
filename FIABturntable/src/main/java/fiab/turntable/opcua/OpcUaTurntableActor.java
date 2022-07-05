package fiab.turntable.opcua;

import akka.actor.ActorRef;
import akka.actor.Props;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames;
import fiab.core.capabilities.transport.TransportModuleCapability;
import fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers;
import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.functionalunit.connector.MachineEventBus;
import fiab.opcua.server.OPCUABase;
import fiab.turntable.TurntableCoordinatorActor;
import fiab.turntable.infrastructure.OpcUaTurntableInfrastructure;
import fiab.turntable.infrastructure.TurntableInfrastructure;
import fiab.turntable.opcua.methods.UaResetTurntable;
import fiab.turntable.opcua.methods.UaStopTurntable;
import fiab.turntable.opcua.methods.UaTransportRequest;
import fiab.turntable.wiring.WiringActor;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

import static fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers.*;

public class OpcUaTurntableActor extends TurntableCoordinatorActor {

    public static Props propsForStandaloneTurntable(OPCUABase base, UaFolderNode rootNode, String machineId) {
        return Props.create(OpcUaTurntableActor.class, () -> new OpcUaTurntableActor(base, rootNode, machineId,
                new MachineEventBus(), new IntraMachineEventBus(), new OpcUaTurntableInfrastructure(base)));
    }

    public static Props props(OPCUABase base, UaFolderNode rootNode, String machineId, MachineEventBus machineEventBus,
                              IntraMachineEventBus intraMachineEventBus, TurntableInfrastructure infrastructure) {
        return Props.create(OpcUaTurntableActor.class, () -> new OpcUaTurntableActor(base, rootNode, machineId,
                machineEventBus, intraMachineEventBus, infrastructure));
    }

    private final OPCUABase base;
    private final UaFolderNode rootNode;
    private final String machineId;
    private UaVariableNode status;
    private ActorRef wiringActor;

    public OpcUaTurntableActor(OPCUABase base, UaFolderNode rootNode, String machineId,
                               MachineEventBus machineEventBus, IntraMachineEventBus intraMachineEventBus, TurntableInfrastructure infrastructure) {
        super(machineEventBus, intraMachineEventBus, infrastructure);
        this.base = base;
        this.rootNode = rootNode;
        this.machineId = machineId;
        this.wiringActor = context().actorOf(WiringActor.props(self(), machineId), machineId+"WiringActor");
        setupOpcUaNodeSet(rootNode);
    }

    protected void setupOpcUaNodeSet(UaFolderNode rootNode) {
        UaMethodNode n1 = base.createPartialMethodNode(rootNode, TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.Reset.toString(), "Requests reset");
        base.addMethodNode(rootNode, n1, new UaResetTurntable(n1, self()));
        UaMethodNode n2 = base.createPartialMethodNode(rootNode, TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.Stop.toString(), "Requests stop");
        base.addMethodNode(rootNode, n2, new UaStopTurntable(n2, self()));
        UaMethodNode n3 = base.createPartialMethodNode(rootNode, TransportModuleCapability.OPCUA_TRANSPORT_REQUEST, "Requests transport");
        base.addMethodNode(rootNode, n3, new UaTransportRequest(n3, self()));
        status = base.generateStringVariableNode(rootNode, OPCUABasicMachineBrowsenames.STATE_VAR_NAME, stateMachine.getState());
        setupTurntableCapabilities();
    }

    private void setupTurntableCapabilities() {
        // add capabilities
        UaFolderNode capabilitiesFolder = base.generateFolder(rootNode, OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITIES);
        UaFolderNode capability1 = base.generateFolder(capabilitiesFolder, OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITY);
        base.generateStringVariableNode(capability1, OPCUACapabilitiesAndWiringInfoBrowsenames.TYPE, TransportModuleCapability.TRANSPORT_CAPABILITY_URI);
        base.generateStringVariableNode(capability1, OPCUACapabilitiesAndWiringInfoBrowsenames.ID, "DefaultTurntableCapabilityInstance");
        base.generateStringVariableNode(capability1, OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE, OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE_VALUE_PROVIDED);
    }

    @Override
    public void setStatusValue(String newStatus) {
        super.setStatusValue(newStatus);
        if (status != null) status.setValue(new DataValue(new Variant(newStatus)));
    }
}