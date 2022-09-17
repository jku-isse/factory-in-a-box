package fiab.plotter.opcua;

import akka.actor.Props;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.functionalunit.BasicFUBehaviour;
import fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames;
import fiab.core.capabilities.transport.TransportModuleCapability;
import fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers;
import fiab.functionalunit.MachineChildFUs;
import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.functionalunit.connector.MachineEventBus;
import fiab.infrastructure.OpcUaMachineChildFUs;
import fiab.opcua.server.OPCUABase;
import fiab.plotter.PlotterCoordinatorActor;
import fiab.plotter.opcua.methods.UaPlotRequest;
import fiab.plotter.opcua.methods.UaResetPlotter;
import fiab.plotter.opcua.methods.UaSetPlotCapability;
import fiab.plotter.opcua.methods.UaStopPlotter;
import fiab.plotter.plotting.PlottingCapability;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

public class OpcUaPlotterCoordinatorActor extends PlotterCoordinatorActor {

    public static Props props(OPCUABase opcuaBase, UaFolderNode rootNode, SupportedColors supportedColor,
                              MachineEventBus machineEventBus, IntraMachineEventBus intraMachineEventBus) {
        return Props.create(OpcUaPlotterCoordinatorActor.class, () ->
                new OpcUaPlotterCoordinatorActor(opcuaBase, rootNode, supportedColor,
                        machineEventBus, intraMachineEventBus, new OpcUaMachineChildFUs(opcuaBase, supportedColor)));
    }

    public static Props propsForStandalonePlotter(OPCUABase opcuaBase, UaFolderNode rootNode, SupportedColors supportedColor) {
        return Props.create(OpcUaPlotterCoordinatorActor.class, () ->
                new OpcUaPlotterCoordinatorActor(opcuaBase, rootNode, supportedColor,
                        new MachineEventBus(), new IntraMachineEventBus(), new OpcUaMachineChildFUs(opcuaBase, supportedColor)));
    }

    private OPCUABase opcuaBase;
    private UaFolderNode rootNode;
    private SupportedColors supportedColor;
    private UaVariableNode statusNode;

    protected OpcUaPlotterCoordinatorActor(OPCUABase opcuaBase, UaFolderNode rootNode, SupportedColors supportedColor,
                                           MachineEventBus machineEventBus, IntraMachineEventBus intraMachineEventBus, MachineChildFUs childFUs) {
        super(machineEventBus, intraMachineEventBus, childFUs);
        this.opcuaBase = opcuaBase;
        this.rootNode = rootNode;
        this.supportedColor = supportedColor;
        setupOpcUaNodeSet(rootNode);
        childFUs.setupInfrastructure(context(), intraMachineEventBus);
    }

    protected void setupOpcUaNodeSet(UaFolderNode rootNode) {
        UaMethodNode resetNode = opcuaBase.createPartialMethodNode(rootNode, OPCUABasicMachineBrowsenames.RESET_REQUEST, "Requests reset");
        opcuaBase.addMethodNode(rootNode, resetNode, new UaResetPlotter(resetNode, self()));
        UaMethodNode stopNode = opcuaBase.createPartialMethodNode(rootNode,  OPCUABasicMachineBrowsenames.STOP_REQUEST, "Requests stop");
        opcuaBase.addMethodNode(rootNode, stopNode, new UaStopPlotter(stopNode, self()));
        UaMethodNode plotNode = opcuaBase.createPartialMethodNode(rootNode, PlottingCapability.PLOT_REQUEST, "Requests plot");
        opcuaBase.addMethodNode(rootNode, plotNode, new UaPlotRequest(plotNode, self()));
        UaMethodNode setCapNode = opcuaBase.createPartialMethodNode(rootNode, OPCUA_SET_CAPABILITY, "Sets color");
        opcuaBase.addMethodNode(rootNode, setCapNode, new UaSetPlotCapability(setCapNode, self()));
        statusNode = opcuaBase.generateStringVariableNode(rootNode, OPCUABasicMachineBrowsenames.STATE_VAR_NAME, stateMachine.getState());
        setupTurntableCapabilities();
    }

    private void setupTurntableCapabilities() {
        // add capabilities
        UaFolderNode capabilitiesFolder = opcuaBase.generateFolder(rootNode, OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITIES);
        UaFolderNode capability1 = opcuaBase.generateFolder(capabilitiesFolder, OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITY);
        opcuaBase.generateStringVariableNode(capability1, OPCUACapabilitiesAndWiringInfoBrowsenames.ID, supportedColor);
        opcuaBase.generateStringVariableNode(capability1, OPCUACapabilitiesAndWiringInfoBrowsenames.TYPE, PLOTTING_CAPABILITY_BASE_URI + supportedColor);
        opcuaBase.generateStringVariableNode(capability1, OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE, OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE_VALUE_PROVIDED);
    }

    @Override
    public void setStatusValue(String newStatus) {
        super.setStatusValue(newStatus);
        if (statusNode != null) statusNode.setValue(new DataValue(new Variant(newStatus)));
    }
}
