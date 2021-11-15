package fiab.mes;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.planer.actor.OrderPlanningActor;
import fiab.mes.transport.actor.transportsystem.HardcodedDefaultTransportRoutingAndMapping;
import fiab.mes.transport.actor.transportsystem.TransportPositionLookup;
import fiab.mes.transport.actor.transportsystem.TransportSystemCoordinatorActor;

public class DefaultShopfloorInfrastructure {

	// create machine and order event bus
	public DefaultShopfloorInfrastructure(ActorSystem system, int expectedTTs) {
		HardcodedDefaultTransportRoutingAndMapping routing = new HardcodedDefaultTransportRoutingAndMapping();
		TransportPositionLookup dns = new TransportPositionLookup();
		ActorRef orderEventBus = system.actorOf(OrderEventBusWrapperActor.props(), OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		ActorRef machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		ActorRef coordActor = system.actorOf(TransportSystemCoordinatorActor.props(routing, dns, expectedTTs), TransportSystemCoordinatorActor.WELLKNOWN_LOOKUP_NAME);
		ActorRef orderPlanningActor = system.actorOf(OrderPlanningActor.props(), OrderPlanningActor.WELLKNOWN_LOOKUP_NAME);
	}
}
