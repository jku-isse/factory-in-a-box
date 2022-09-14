package fiab.mes.assembly.order.actor;

import fiab.mes.eventbus.*;
import fiab.mes.planer.actor.MachineOrderMappingManager;
import fiab.mes.planer.actor.OrderPlanningActor;

public class BikeAssemblyOrderPlanningActor extends OrderPlanningActor {

    @Override
    protected void getEventBusAndSubscribe() throws Exception {
        //.getEventBusAndSubscribe();
        SubscribeMessage orderSub = new SubscribeMessage(getSelf(), new MESSubscriptionClassifier(WELLKNOWN_LOOKUP_NAME, "*"));
        orderEventBus = this.context().actorSelection("/user/" + AssemblyMonitoringEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        orderEventBus.tell(orderSub, getSelf());

        SubscribeMessage machineSub = new SubscribeMessage(getSelf(), new MESSubscriptionClassifier(WELLKNOWN_LOOKUP_NAME, "*"));
        machineEventBus = this.context().actorSelection("/user/" + InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        machineEventBus.tell(machineSub, getSelf());

        ordMapper = new MachineOrderMappingManager(orderEventBus, self().path().name());
    }


}
