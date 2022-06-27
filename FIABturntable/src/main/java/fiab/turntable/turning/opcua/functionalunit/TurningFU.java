package fiab.turntable.turning.opcua.functionalunit;

import akka.actor.Props;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.functionalunit.connector.FUConnector;
import fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames;
import fiab.opcua.server.OPCUABase;
import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.turntable.turning.TurningActor;
import fiab.turntable.turning.TurningCapability;
import fiab.turntable.turning.opcua.methods.UaRequestTurning;
import fiab.turntable.turning.opcua.methods.UaResetTurning;
import fiab.turntable.turning.opcua.methods.UaStopTurning;
import fiab.turntable.turning.statemachine.TurningStates;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

public class TurningFU extends TurningActor {

    public static Props props(OPCUABase base, UaFolderNode root, FUConnector turningConnector, IntraMachineEventBus intraMachineEventBus) {
        return Props.create(TurningFU.class, () -> new TurningFU(base, root, turningConnector, intraMachineEventBus));
    }

    UaFolderNode rootNode;
    String fuPrefix;
    OPCUABase base;

    private UaVariableNode status = null;

    public TurningFU(OPCUABase base, UaFolderNode root, FUConnector turningConnector, IntraMachineEventBus intraEventBus) {
        super(turningConnector, intraEventBus);
        this.base = base;
        this.rootNode = root;
        this.fuPrefix = createBasePathForFU(rootNode);
        setupOPCUANodeSet();
    }

    private String createBasePathForFU(UaFolderNode rootNode) {
        return rootNode.getNodeId().getIdentifier() + "/" + TurningCapability.TURNING_ID;
    }

    private void setupOPCUANodeSet() {
        UaFolderNode turningFuNode = base.generateFolder(rootNode, TurningCapability.TURNING_ID);

        status = base.generateStringVariableNode(turningFuNode,
                OPCUABasicMachineBrowsenames.STATE_VAR_NAME, TurningStates.STOPPED);

        addTurningOpcUaMethods(turningFuNode);
        addCapabilities(turningFuNode);
    }

    private void addTurningOpcUaMethods(UaFolderNode turningFuNode){
        UaMethodNode resetNode = base.createPartialMethodNode(turningFuNode,
              TurningCapability.RESET_REQUEST , "Requests reset");
        base.addMethodNode(turningFuNode, resetNode, new UaResetTurning(resetNode, self()));
        UaMethodNode stopNode = base.createPartialMethodNode(turningFuNode,
                TurningCapability.STOP_REQUEST, "Requests stop");
        base.addMethodNode(turningFuNode, stopNode, new UaStopTurning(stopNode, self()));
        UaMethodNode turnNode = base.createPartialMethodNode(turningFuNode,
                TurningCapability.TURN_TO_REQUEST, "Requests turning");
        base.addMethodNode(turningFuNode, turnNode, new UaRequestTurning(turnNode, self()));
    }

    private void addCapabilities(UaFolderNode turningFuNode){
        UaFolderNode capabilitiesFolder = base.generateFolder(turningFuNode,
                OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITIES);
        addTurningCapability(capabilitiesFolder);
    }

    private void addTurningCapability(UaFolderNode capabilitiesFolder){
        UaFolderNode turningCapNode = base.generateFolder(capabilitiesFolder,
                OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITY);

        base.generateStringVariableNode(turningCapNode,
                OPCUACapabilitiesAndWiringInfoBrowsenames.TYPE,
                TurningCapability.OPC_UA_BASE_URI);
        base.generateStringVariableNode(turningCapNode,
                OPCUACapabilitiesAndWiringInfoBrowsenames.ID,
                TurningCapability.CAPABILITY_ID);
        base.generateStringVariableNode(turningCapNode,
                OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE,
                OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE_VALUE_PROVIDED);
    }

    @Override
    public void setStatusValue(String newStatus) {
        if (status != null) {
            status.setValue(new DataValue(new Variant(newStatus)));
        }
    }
}
