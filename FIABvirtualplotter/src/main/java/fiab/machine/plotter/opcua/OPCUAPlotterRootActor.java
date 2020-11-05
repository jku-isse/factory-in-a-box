package fiab.machine.plotter.opcua;

import fiab.machine.plotter.MachineCapabilityUpdateEvent;
import fiab.machine.plotter.opcua.methods.SetCapability;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames;
import fiab.core.capabilities.plotting.PlotterMessageTypes;
import fiab.core.capabilities.plotting.WellknownPlotterCapability;
import fiab.core.capabilities.plotting.WellknownPlotterCapability.SupportedColors;
import fiab.handshake.fu.HandshakeFU;
import fiab.handshake.fu.server.ServerSideHandshakeFU;
import fiab.machine.plotter.IntraMachineEventBus;
import fiab.machine.plotter.VirtualPlotterCoordinatorActor;
import fiab.machine.plotter.opcua.methods.PlotRequest;
import fiab.machine.plotter.opcua.methods.Reset;
import fiab.machine.plotter.opcua.methods.Stop;
import fiab.opcua.server.NonEncryptionBaseOpcUaServer;
import fiab.opcua.server.OPCUABase;
import config.HardwareInfo;
import config.MachineType;

public class OPCUAPlotterRootActor extends AbstractActor {

    private String machineName = "Plotter";
    static final String NAMESPACE_URI = "urn:factory-in-a-box";
    private UaVariableNode status = null;
    private UaVariableNode capability = null;
    private ActorRef plotterCoordinator;
    private SupportedColors color;
    private int portOffset;

    static public Props props(String machineName, int portOffset, SupportedColors color) {
        return Props.create(OPCUAPlotterRootActor.class, () -> new OPCUAPlotterRootActor(machineName, portOffset, color));
    }

    public OPCUAPlotterRootActor(String machineName, int portOffset, SupportedColors color) {
        try {
            this.machineName = machineName;
            this.color = color;
            this.portOffset = portOffset;
            init();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public Receive createReceive() {

        return receiveBuilder()
                .match(MachineCapabilityUpdateEvent.class, req -> {
                    setPlotCapability(req.getValue().toString());
                })
                .match(MachineStatusUpdateEvent.class, req -> {
                    setStatusValue(req.getStatus().toString());
                })
                .build();
    }


    private void init() throws Exception {
        NonEncryptionBaseOpcUaServer server1 = new NonEncryptionBaseOpcUaServer(portOffset, machineName);

        OPCUABase opcuaBase = new OPCUABase(server1.getServer(), NAMESPACE_URI, machineName);
        UaFolderNode root = opcuaBase.prepareRootNode();
        UaFolderNode plotterNode = opcuaBase.generateFolder(root, machineName, "Plotting_FU");
        String fuPrefix = machineName + "/" + "Plotting_FU";

        IntraMachineEventBus intraEventBus = new IntraMachineEventBus();
        intraEventBus.subscribe(getSelf(), new fiab.machine.plotter.SubscriptionClassifier("Plotter Module", "*"));
        plotterCoordinator = context().actorOf(VirtualPlotterCoordinatorActor.propsForLateHandshakeBinding(intraEventBus), machineName);
        plotterCoordinator.tell(PlotterMessageTypes.SubscribeState, getSelf());
        
        UaFolderNode plotHardwareNode = opcuaBase.generateFolder(plotterNode, fuPrefix, "Hardware");
        opcuaBase.generateStringVariableNode(plotHardwareNode, fuPrefix + "/Hardware", "PlotXMotor", machineName +"/Hardware/Elements/MotorB");
        opcuaBase.generateStringVariableNode(plotHardwareNode, fuPrefix + "/Hardware", "PlotYMotor", machineName +"/Hardware/Elements/MotorC");
        opcuaBase.generateStringVariableNode(plotHardwareNode, fuPrefix + "/Hardware", "PenMotor", machineName +"/Hardware/Elements/MotorD");
        opcuaBase.generateStringVariableNode(plotHardwareNode, fuPrefix + "/Hardware", "HomingXSensor", machineName +"/Hardware/Elements/Sensor4");
        opcuaBase.generateStringVariableNode(plotHardwareNode, fuPrefix + "/Hardware", "HomingYSensor", machineName +"/Hardware/Elements/Sensor3");
        
        HardwareInfo hardwareInfo = new HardwareInfo(MachineType.PLOTTER);
        context().actorOf(OPCUAPlotterHardwareMonitor.props(opcuaBase, root, machineName, hardwareInfo));
        
        UaFolderNode conveyorNode = opcuaBase.generateFolder(root, machineName, "Conveyor_FU");
        UaFolderNode convHardwareNode = opcuaBase.generateFolder(conveyorNode, machineName + "/Conveyor_FU",  "Hardware");
        opcuaBase.generateStringVariableNode(convHardwareNode, machineName + "/Conveyor_FU/Hardware", "ConveyorMotor", machineName + "/Hardware/Elements/MotorA");
        opcuaBase.generateStringVariableNode(convHardwareNode, machineName + "/Conveyor_FU/Hardware", "SensorUnloading", machineName + "/Hardware/Elements/Sensor1");
        opcuaBase.generateStringVariableNode(convHardwareNode, machineName + "/Conveyor_FU/Hardware", "SensorLoading", machineName + "/Hardware/Elements/Sensor2");
        
        
        UaFolderNode hsNode = opcuaBase.generateFolder(root, machineName, "Handshake_FU");
        HandshakeFU defaultHandshakeFU = new ServerSideHandshakeFU(opcuaBase, hsNode, fuPrefix + "/Handshake_FU", plotterCoordinator, getContext(), "DefaultServerSideHandshake", OPCUACapabilitiesAndWiringInfoBrowsenames.IS_PROVIDED, true);
        //ActorRef serverSide = defaultHandshakeFU.getFUActor();
        //		.setupOPCUANodeSet(plotterWrapper, opcuaBase, ttNode, fuPrefix, getContext());
        //plotterCoordinator.tell(serverSide, getSelf());

        setupPlotterCapabilities(opcuaBase, plotterNode, fuPrefix, color);
        setupOPCUANodeSet(opcuaBase, plotterNode, fuPrefix, plotterCoordinator);

        Thread s1 = new Thread(opcuaBase);
        s1.start();
    }


    private void setupOPCUANodeSet(OPCUABase opcuaBase, UaFolderNode ttNode, String path, ActorRef plotterActor) {
        //TODO rename ttNode
        UaMethodNode n1 = opcuaBase.createPartialMethodNode(path, PlotterMessageTypes.Reset.toString(), "Requests reset");
        opcuaBase.addMethodNode(ttNode, n1, new Reset(n1, plotterActor));
        UaMethodNode n2 = opcuaBase.createPartialMethodNode(path, PlotterMessageTypes.Stop.toString(), "Requests stop");
        opcuaBase.addMethodNode(ttNode, n2, new Stop(n2, plotterActor));
        UaMethodNode n3 = opcuaBase.createPartialMethodNode(path, PlotterMessageTypes.Plot.toString(), "Requests plot");
        opcuaBase.addMethodNode(ttNode, n3, new PlotRequest(n3, plotterActor));
        UaMethodNode n4 = opcuaBase.createPartialMethodNode(path, PlotterMessageTypes.SetCapability.toString(), "Sets Plotting Capability");
        opcuaBase.addMethodNode(ttNode, n4, new SetCapability(n2, plotterActor));
        status = opcuaBase.generateStringVariableNode(ttNode, path, OPCUABasicMachineBrowsenames.STATE_VAR_NAME, BasicMachineStates.UNKNOWN);
    }

    private void setupPlotterCapabilities(OPCUABase opcuaBase, UaFolderNode ttNode, String path, SupportedColors color) {
        // add capabilities
        UaFolderNode capabilitiesFolder = opcuaBase.generateFolder(ttNode, path, new String(OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITIES));
        path = path + "/" + OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITIES;
        UaFolderNode capability1 = opcuaBase.generateFolder(capabilitiesFolder, path,
                "CAPABILITY", OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITY);
        capability = opcuaBase.generateStringVariableNode(capability1, path + "/CAPABILITY", OPCUACapabilitiesAndWiringInfoBrowsenames.TYPE,
                WellknownPlotterCapability.generatePlottingCapabilityURI(color));
        opcuaBase.generateStringVariableNode(capability1, path + "/CAPABILITY", OPCUACapabilitiesAndWiringInfoBrowsenames.ID,
                "DefaultPlotterCapabilityInstance");
        opcuaBase.generateStringVariableNode(capability1, path + "/CAPABILITY", OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE,
                OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE_VALUE_PROVIDED);
    }

    private void setStatusValue(String newStatus) {
        if (status != null) {
            status.setValue(new DataValue(new Variant(newStatus)));
        }
    }

    private void setPlotCapability(String newCapability) {
        if (capability != null) {
            capability.setValue(new DataValue(new Variant(newCapability)));
        }
    }
}
