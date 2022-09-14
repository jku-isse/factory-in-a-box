package fiab.mes.assembly.order.actor;

import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.order.actor.OrderEntryActor;

public class BikeAssemblyOrderEntryActor extends OrderEntryActor {

    public BikeAssemblyOrderEntryActor(){
        eventBusByRef = context().actorSelection("/user/"+ OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        orderPlannerByRef = context().actorSelection("/user/"+ BikeAssemblyOrderPlanningActor.WELLKNOWN_LOOKUP_NAME);
        eventBusByRef.tell(new SubscribeMessage(getSelf(), new MESSubscriptionClassifier(self().path().name(), "*")), getSelf());
    }
}
