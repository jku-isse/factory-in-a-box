package fiab.mes;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.order.actor.OrderEntryActor;
import fiab.mes.planer.actor.OrderPlanningActor;

public class DefaultShopfloorInfrastructure {

	// create machine and order event bus
	public DefaultShopfloorInfrastructure(ActorSystem system) {
		ActorRef orderEventBus = system.actorOf(OrderEventBusWrapperActor.props(), OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		ActorRef machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		ActorRef orderPlanningActor = system.actorOf(OrderPlanningActor.props(), OrderPlanningActor.WELLKNOWN_LOOKUP_NAME);
	}
}
