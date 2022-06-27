package fiab.plotter.plotting.opcua.functionalunit;

import akka.actor.Props;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames;
import fiab.core.capabilities.plotting.WellknownPlotterCapability;
import fiab.functionalunit.connector.FUConnector;
import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.opcua.server.OPCUABase;
import fiab.plotter.plotting.PlotterActor;
import fiab.plotter.plotting.PlottingCapability;
import fiab.plotter.plotting.opcua.methods.UaRequestPlotting;
import fiab.plotter.plotting.opcua.methods.UaResetPlotting;
import fiab.plotter.plotting.opcua.methods.UaStopPlotting;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

import java.util.Locale;

public class PlotterFU extends PlotterActor {

    public static Props props(OPCUABase opcuaBase, UaFolderNode rootNode, WellknownPlotterCapability.SupportedColors color,
                              FUConnector plottingConnector, IntraMachineEventBus intraMachineBus) {
        return Props.create(PlotterFU.class, () -> new PlotterFU(opcuaBase, rootNode, color, plottingConnector, intraMachineBus));
    }

    OPCUABase opcuaBase;
    UaFolderNode rootNode;
    String fuPrefix;
    WellknownPlotterCapability.SupportedColors color;

    private UaVariableNode status = null;

    public PlotterFU(OPCUABase opcuaBase, UaFolderNode rootNode, WellknownPlotterCapability.SupportedColors color,
                     FUConnector plottingConnector, IntraMachineEventBus intraMachineBus) {
        super(plottingConnector, intraMachineBus);
        this.opcuaBase = opcuaBase;
        this.rootNode = rootNode;
        this.color = color;
        this.fuPrefix = createBasePathForFU(rootNode);
        setupOPCUANodeSet();
    }

    private String createBasePathForFU(UaFolderNode rootNode) {
        return rootNode.getNodeId().getIdentifier() + "/" + PlottingCapability.PlOTTING_ID;
    }

    private void setupOPCUANodeSet() {
        UaFolderNode turningFuNode = opcuaBase.generateFolder(rootNode, PlottingCapability.PlOTTING_ID);

        status = opcuaBase.generateStringVariableNode(turningFuNode,
                OPCUABasicMachineBrowsenames.STATE_VAR_NAME, BasicMachineStates.STOPPED);

        addTurningOpcUaMethods(turningFuNode);
        addCapabilities(turningFuNode);
    }

    private void addTurningOpcUaMethods(UaFolderNode plottingFuNode) {
        UaMethodNode resetNode = opcuaBase.createPartialMethodNode(plottingFuNode,
                PlottingCapability.RESET_REQUEST, "Requests reset");
        opcuaBase.addMethodNode(plottingFuNode, resetNode, new UaResetPlotting(resetNode, self()));
        UaMethodNode stopNode = opcuaBase.createPartialMethodNode(plottingFuNode,
                PlottingCapability.STOP_REQUEST, "Requests stop");
        opcuaBase.addMethodNode(plottingFuNode, stopNode, new UaStopPlotting(stopNode, self()));
        UaMethodNode turnNode = opcuaBase.createPartialMethodNode(plottingFuNode,
                PlottingCapability.PLOT_REQUEST, "Requests plotting");
        opcuaBase.addMethodNode(plottingFuNode, turnNode, new UaRequestPlotting(turnNode, self()));
    }

    private void addCapabilities(UaFolderNode plottingFuNode) {
        UaFolderNode capabilitiesFolder = opcuaBase.generateFolder(plottingFuNode,
                OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITIES);
        addTurningCapability(capabilitiesFolder);
    }

    private void addTurningCapability(UaFolderNode capabilitiesFolder) {
        UaFolderNode plottingCapNode = opcuaBase.generateFolder(capabilitiesFolder,
                OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITY);

        opcuaBase.generateStringVariableNode(plottingCapNode,
                OPCUACapabilitiesAndWiringInfoBrowsenames.TYPE ,
                PlottingCapability.OPC_UA_BASE_URI + color.name());
        opcuaBase.generateStringVariableNode(plottingCapNode,
                OPCUACapabilitiesAndWiringInfoBrowsenames.ID,
                PlottingCapability.CAPABILITY_ID);
        opcuaBase.generateStringVariableNode(plottingCapNode,
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
