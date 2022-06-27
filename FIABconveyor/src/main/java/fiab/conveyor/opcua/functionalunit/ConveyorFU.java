package fiab.conveyor.opcua.functionalunit;

import akka.actor.Props;
import fiab.conveyor.ConveyorCapability;
import fiab.conveyor.opcua.methods.UaLoadConveyor;
import fiab.conveyor.opcua.methods.UaUnloadConveyor;
import fiab.conveyor.statemachine.ConveyorStates;
import fiab.functionalunit.connector.FUConnector;
import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.conveyor.opcua.methods.UaResetConveyor;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames;
import fiab.opcua.server.OPCUABase;
import fiab.conveyor.ConveyorActor;
import fiab.conveyor.opcua.methods.UaStopConveyor;

public class ConveyorFU extends ConveyorActor {

    public static Props props(OPCUABase base, UaFolderNode root, FUConnector conveyorConnector, IntraMachineEventBus intraEventBus) {
        return Props.create(ConveyorFU.class, () -> new ConveyorFU(base, root, conveyorConnector, intraEventBus));
    }

    protected UaFolderNode rootNode;
    protected String fuPrefix;
    protected OPCUABase base;

    private UaVariableNode uaStatusNode = null;

    public ConveyorFU(OPCUABase base, UaFolderNode root, FUConnector conveyorConnector, IntraMachineEventBus intraEventBus) {
        super(conveyorConnector, intraEventBus);
        this.base = base;
        this.rootNode = root;
        this.fuPrefix = createBasePathForFU(rootNode);
        setupOPCUANodeSet();
    }

    private String createBasePathForFU(UaFolderNode rootNode) {
        return rootNode.getNodeId().getIdentifier() + "/" + ConveyorCapability.CONVEYOR_ID;
    }

    private void setupOPCUANodeSet() {
        log.info("Setting up opcua nodes");
        UaFolderNode conveyorFuNode = base.generateFolder(rootNode, ConveyorCapability.CONVEYOR_ID);

        uaStatusNode = base.generateStringVariableNode(conveyorFuNode, OPCUABasicMachineBrowsenames.STATE_VAR_NAME,
                ConveyorStates.STOPPED);

        addInternalControlsOpcUa(conveyorFuNode);
        // add capabilities
        addCapabilities(conveyorFuNode);
    }

    private void addInternalControlsOpcUa(UaFolderNode handshakeNode) {
        UaMethodNode uaResetNode = base.createPartialMethodNode(handshakeNode, ConveyorCapability.RESET_REQUEST, "Requests reset");
        base.addMethodNode(handshakeNode, uaResetNode, new UaResetConveyor(uaResetNode, self()));

        UaMethodNode uaStopNode = base.createPartialMethodNode(handshakeNode, ConveyorCapability.STOP_REQUEST, "Requests stop");
        base.addMethodNode(handshakeNode, uaStopNode, new UaStopConveyor(uaStopNode, self()));

        UaMethodNode uaLoadNode = base.createPartialMethodNode(handshakeNode, ConveyorCapability.LOAD_REQUEST, "Requests load");
        base.addMethodNode(handshakeNode, uaLoadNode, new UaLoadConveyor(uaLoadNode, self()));

        UaMethodNode uaUnloadNode = base.createPartialMethodNode(handshakeNode, ConveyorCapability.UNLOAD_REQUEST, "Requests unload");
        base.addMethodNode(handshakeNode, uaUnloadNode, new UaUnloadConveyor(uaUnloadNode, self()));
    }

    private void addCapabilities(UaFolderNode conveyorFuNode) {
        UaFolderNode capabilitiesFolder = base.generateFolder(conveyorFuNode,
                OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITIES);
        addConveyorCapability(capabilitiesFolder);
    }

    private void addConveyorCapability(UaFolderNode capabilitiesFolder) {
        UaFolderNode capability1 = base.generateFolder(capabilitiesFolder,
                OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITY);

        base.generateStringVariableNode(capability1,
                OPCUACapabilitiesAndWiringInfoBrowsenames.TYPE, ConveyorCapability.OPC_UA_BASE_URI);

        base.generateStringVariableNode(capability1,
                OPCUACapabilitiesAndWiringInfoBrowsenames.ID, ConveyorCapability.CAPABILITY_ID);

        base.generateStringVariableNode(capability1,
                OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE,
                OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE_VALUE_PROVIDED);
    }

	
	/*public ActorRef getActor() {
		//return conveyingActor;
	}*/

    @Override
    public void setStatusValue(String newStatus) {
        if (uaStatusNode != null) {
            uaStatusNode.setValue(new DataValue(new Variant(newStatus)));
        }
    }
}
