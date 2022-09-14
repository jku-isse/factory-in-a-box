package fiab.plotter;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import fiab.core.capabilities.plotting.WellknownPlotterCapability;
import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.functionalunit.connector.MachineEventBus;
import fiab.opcua.server.OPCUABase;
import fiab.plotter.opcua.OpcUaPlotterCoordinatorActor;

public class PlotterFactory {

    public static ActorRef startupStandalonePlotter(int port, String name) {
        ActorSystem system = ActorSystem.create("PlotterActorSystem");
        OPCUABase opcuaBase = OPCUABase.createAndStartLocalServer(port, name);
        ActorRef plotter = system.actorOf(OpcUaPlotterCoordinatorActor.props(opcuaBase, opcuaBase.getRootNode(),
                WellknownPlotterCapability.SupportedColors.BLACK, new MachineEventBus(), new IntraMachineEventBus()), name);
        return plotter;
    }

    public static ActorRef startupStandalonePlotter(int port, MachineEventBus machineEventBus, String name) {
        ActorSystem system = ActorSystem.create("PlotterActorSystem");
        OPCUABase opcuaBase = OPCUABase.createAndStartLocalServer(4840, "Plotter");
        ActorRef plotter = system.actorOf(OpcUaPlotterCoordinatorActor.props(opcuaBase, opcuaBase.getRootNode(),
                WellknownPlotterCapability.SupportedColors.BLACK, machineEventBus, new IntraMachineEventBus()), "TestPlotter");
        return plotter;
    }
}
