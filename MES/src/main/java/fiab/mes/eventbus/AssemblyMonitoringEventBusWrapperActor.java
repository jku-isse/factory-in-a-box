package fiab.mes.eventbus;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.core.capabilities.basicmachine.events.MachineEvent;
import fiab.functionalunit.connector.MachineEventBus;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.eventbus.UnsubscribeMessage;
import fiab.mes.machine.msg.OrderRelocationNotification;
import fiab.mes.order.msg.RegisterProcessRequest;

public class AssemblyMonitoringEventBusWrapperActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    public static final String WRAPPER_ACTOR_LOOKUP_NAME = "AssemblyMonitoringEventBusWrapperActor";
    private final AssemblyMonitoringEventBus meb;

    public AssemblyMonitoringEventBusWrapperActor(AssemblyMonitoringEventBus bus) {
        meb = bus == null ? new AssemblyMonitoringEventBus() : bus;
    }

    public static Props props() {
        return Props.create(AssemblyMonitoringEventBusWrapperActor.class, () -> new AssemblyMonitoringEventBusWrapperActor(null));
    }

    public static Props propsWithPreparedBus(AssemblyMonitoringEventBus bus) {
        return Props.create(AssemblyMonitoringEventBusWrapperActor.class, () -> new AssemblyMonitoringEventBusWrapperActor(bus));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(SubscribeMessage.class, msg -> {
                    log.info("Subscribe from: " + msg.getSubscriber().path().toString());
                    meb.subscribe(msg.getSubscriber(), msg.getSubscriptionClassifier());
                })
                .match(UnsubscribeMessage.class, msg -> {
                    log.info("Unsubscribe from: " + msg.getSubscriber().path().toString());
                    if (msg.getSubscriptionClassifier() == null) {
                        meb.unsubscribe(msg.getSubscriber());
                    } else {
                        meb.unsubscribe(msg.getSubscriber(), msg.getSubscriptionClassifier());
                    }
                })
                .match(RegisterProcessRequest.class, msg -> {
                    log.debug("Received Publish Event: " + msg.toString());
                    meb.publish(msg);
                })
                .build();
    }

}
