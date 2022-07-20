package fiab.infrastructure;

import akka.actor.ActorContext;
import fiab.conveyor.ConveyorActor;
import fiab.conveyor.ConveyorCapability;
import fiab.conveyor.opcua.functionalunit.ConveyorFU;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.core.capabilities.plotting.WellknownPlotterCapability;
import fiab.functionalunit.MachineChildFUs;
import fiab.functionalunit.connector.FUConnector;
import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.handshake.connector.ServerNotificationConnector;
import fiab.handshake.connector.ServerResponseConnector;
import fiab.handshake.server.ServerSideHandshakeActor;
import fiab.handshake.server.opcua.functionalunit.ServerHandshakeFU;
import fiab.opcua.server.OPCUABase;
import fiab.plotter.plotting.PlotterActor;
import fiab.plotter.plotting.PlottingCapability;
import fiab.plotter.plotting.opcua.functionalunit.PlotterFU;

public class OpcUaMachineChildFUs extends MachineChildFUs {

    private OPCUABase base;
    private ActorContext context;
    private IntraMachineEventBus intraMachineEventBus;
    private WellknownPlotterCapability.SupportedColors supportedColor;

    public OpcUaMachineChildFUs(OPCUABase base, WellknownPlotterCapability.SupportedColors supportedColor) {
        super();
        this.base = base;
        this.supportedColor = supportedColor;
    }

    @Override
    public void setupInfrastructure(ActorContext context, IntraMachineEventBus intraMachineEventBus) {
        this.context = context;
        this.intraMachineEventBus = intraMachineEventBus;
        createAndLinkConveyorFU();
        createAndLinkPlottingFU();
        createAndLinkServerHandshakeFU();
    }

    public void createAndLinkConveyorFU() {
        String capId = ConveyorCapability.CAPABILITY_ID;
        FUConnector conveyorConnector = new FUConnector();
        context.actorOf(ConveyorFU.props(base, base.getRootNode(), conveyorConnector, intraMachineEventBus), capId);
        this.fuConnectors.put(capId, conveyorConnector);
    }

    public void createAndLinkPlottingFU() {
        String capId = PlottingCapability.CAPABILITY_ID;
        FUConnector plottingConnector = new FUConnector();
        context.actorOf(PlotterFU.props(base, base.getRootNode(), supportedColor, plottingConnector, intraMachineEventBus));
        this.fuConnectors.put(capId, plottingConnector);
    }

    public void createAndLinkServerHandshakeFU() {
        String capId = HandshakeCapability.SERVER_CAPABILITY_ID;
        FUConnector handshakeConnector = new FUConnector();
        context.actorOf(ServerHandshakeFU.props(base, base.getRootNode(), "",
                handshakeConnector, intraMachineEventBus));
        this.fuConnectors.put(capId, handshakeConnector);
    }
}
