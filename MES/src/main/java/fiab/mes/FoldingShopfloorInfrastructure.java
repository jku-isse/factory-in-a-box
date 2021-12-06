package fiab.mes;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.planer.actor.OrderPlanningActor;
import fiab.mes.transport.actor.transportsystem.*;

public class FoldingShopfloorInfrastructure {

    // create machine and order event bus
    public FoldingShopfloorInfrastructure(ActorSystem system, int expectedTTs) {
        HardcodedFoldingTransportRoutingAndMapping routing = new HardcodedFoldingTransportRoutingAndMapping();
        FoldingTransportPositionLookup dns = new FoldingTransportPositionLookup();
        ActorRef orderEventBus = system.actorOf(OrderEventBusWrapperActor.props(), OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        ActorRef machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        ActorRef coordActor = system.actorOf(TransportSystemCoordinatorActor.props(routing, dns, expectedTTs), TransportSystemCoordinatorActor.WELLKNOWN_LOOKUP_NAME);
        ActorRef orderPlanningActor = system.actorOf(OrderPlanningActor.props(), OrderPlanningActor.WELLKNOWN_LOOKUP_NAME);
    }
}
