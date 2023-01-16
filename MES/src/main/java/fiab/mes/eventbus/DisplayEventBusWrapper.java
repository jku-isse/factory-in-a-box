package fiab.mes.eventbus;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.mes.assembly.display.msg.DisplayRequest;
import fiab.mes.order.msg.RegisterProcessRequest;

public class DisplayEventBusWrapper extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    public static final String WRAPPER_ACTOR_LOOKUP_NAME = "DisplayEventBusWrapperActor";
    private final DisplayEventBus meb;

    public DisplayEventBusWrapper(DisplayEventBus bus) {
        meb = bus == null ? new DisplayEventBus() : bus;
    }

    public static Props props() {
        return Props.create(DisplayEventBusWrapper.class, () -> new DisplayEventBusWrapper(null));
    }

    public static Props propsWithPreparedBus(DisplayEventBus bus) {
        return Props.create(DisplayEventBusWrapper.class, () -> new DisplayEventBusWrapper(bus));
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
                .match(DisplayRequest.class, msg -> {
                    log.debug("Received Publish Event: " + msg.toString());
                    meb.publish(msg);
                })
                .build();
    }
}
