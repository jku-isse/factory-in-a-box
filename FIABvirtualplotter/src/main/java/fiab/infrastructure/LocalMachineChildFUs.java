package fiab.infrastructure;

import akka.actor.ActorContext;
import fiab.conveyor.ConveyorActor;
import fiab.conveyor.ConveyorCapability;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.functionalunit.MachineChildFUs;
import fiab.functionalunit.connector.FUConnector;
import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.handshake.connector.ServerNotificationConnector;
import fiab.handshake.connector.ServerResponseConnector;
import fiab.handshake.server.ServerSideHandshakeActor;
import fiab.handshake.server.opcua.functionalunit.ServerHandshakeFU;
import fiab.plotter.plotting.PlotterActor;
import fiab.plotter.plotting.PlottingCapability;

public class LocalMachineChildFUs extends MachineChildFUs {

    private ActorContext context;
    private IntraMachineEventBus intraMachineEventBus;

    public LocalMachineChildFUs() {
        super();
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
        context.actorOf(ConveyorActor.props(conveyorConnector, intraMachineEventBus), capId);
        this.fuConnectors.put(capId, conveyorConnector);
    }

    public void createAndLinkPlottingFU() {
        String capId = PlottingCapability.CAPABILITY_ID;
        FUConnector plottingConnector = new FUConnector();
        context.actorOf(PlotterActor.props(plottingConnector, intraMachineEventBus));
        this.fuConnectors.put(capId, plottingConnector);
    }

    public void createAndLinkServerHandshakeFU() {
        String capId = HandshakeCapability.SERVER_CAPABILITY_ID;
        FUConnector handshakeConnector = new FUConnector();
        //There should ba a way to pass the client fu in the constructor, since this way no client messages can be received
        context.actorOf(ServerSideHandshakeActor.props(handshakeConnector, new IntraMachineEventBus(), new ServerResponseConnector(), new ServerNotificationConnector()));
        this.fuConnectors.put(capId, handshakeConnector);
    }
}
