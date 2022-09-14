package fiab.mes.assembly.monitoring.actor;

import akka.actor.AbstractActor;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import fiab.mes.eventbus.*;
import fiab.mes.order.msg.OrderEvent;

public class AssemblyMonitoringActor extends AbstractActor {

    protected ActorSelection orderEventBus;
    protected ActorSelection monitoringEventBus;

    public static final String WELLKNOWN_LOOKUP_NAME = "AssemblyMonitoringActor";

    public static Props props(){
        return Props.create(AssemblyMonitoringActor.class, () -> new AssemblyMonitoringActor());
    }

    public AssemblyMonitoringActor(){
        getEventBusAndSubscribe();
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(OrderEvent.class, req -> monitoringEventBus.forward(req, context()))
                .build();
    }

    protected void getEventBusAndSubscribe() {
        //.getEventBusAndSubscribe();
        SubscribeMessage orderSub = new SubscribeMessage(getSelf(), new MESSubscriptionClassifier(WELLKNOWN_LOOKUP_NAME, "*"));
        orderEventBus = this.context().actorSelection("/user/" + AssemblyMonitoringEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        orderEventBus.tell(orderSub, getSelf());

        SubscribeMessage machineSub = new SubscribeMessage(getSelf(), new MESSubscriptionClassifier(WELLKNOWN_LOOKUP_NAME, "*"));
        monitoringEventBus = this.context().actorSelection("/user/" + OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        monitoringEventBus.tell(machineSub, getSelf());
    }
}
