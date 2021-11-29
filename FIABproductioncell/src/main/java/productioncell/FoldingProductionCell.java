package productioncell;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import fiab.mes.ShopfloorConfigurations;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.transport.actor.transportsystem.HardcodedDefaultTransportRoutingAndMapping;
import fiab.mes.transport.actor.transportsystem.DefaultTransportPositionLookup;
import fiab.mes.transport.actor.transportsystem.TransportSystemCoordinatorActor;
import productioncell.foldingstation.FoldingProductionCellCoordinator;

public class FoldingProductionCell {

    private static String production_cell_name;
    private static ActorSystem system;

    public static void main(String[] args) {
        if(args.length > 0) {
            production_cell_name = args[0];
        }else{
            production_cell_name = "LocalFoldingProductionCell";
        }
        system = ActorSystem.create(production_cell_name);
        HardcodedDefaultTransportRoutingAndMapping routing = new HardcodedDefaultTransportRoutingAndMapping();
        DefaultTransportPositionLookup dns = new DefaultTransportPositionLookup();
        ActorRef machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        ActorRef transportCoord = system.actorOf(TransportSystemCoordinatorActor.props(routing, dns, 1), TransportSystemCoordinatorActor.WELLKNOWN_LOOKUP_NAME);
        ActorRef foldingCellCoord = system.actorOf(FoldingProductionCellCoordinator.props(), FoldingProductionCellCoordinator.WELLKNOWN_LOOKUP_NAME);
        loadProductionCellLayoutFromFile();
    }

    public static void loadProductionCellLayoutFromFile(){
        ShopfloorConfigurations.JsonFilePersistedDiscovery discovery = new ShopfloorConfigurations.JsonFilePersistedDiscovery(production_cell_name);
        discovery.triggerDiscoveryMechanism(system);
    }

}
