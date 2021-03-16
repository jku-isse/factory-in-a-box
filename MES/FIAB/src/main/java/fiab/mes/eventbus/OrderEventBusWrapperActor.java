package fiab.mes.eventbus;

import akka.actor.AbstractActor.Receive;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.mes.order.msg.OrderEvent;
import fiab.tracing.actor.AbstractTracingActor;

public class OrderEventBusWrapperActor extends AbstractTracingActor {
	
	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	
	public static final String WRAPPER_ACTOR_LOOKUP_NAME = "OrderEventBusWrapperActor";
	private OrderEventBus oeb;

	public OrderEventBusWrapperActor() {
		oeb = new OrderEventBus();
	}

	public static Props props() {
		return Props.create(OrderEventBusWrapperActor.class, () -> new OrderEventBusWrapperActor());
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(SubscribeMessage.class, msg -> {
					log.info("Subscribe from: "+msg.getSubscriber().path().toString());
					oeb.subscribe(msg.getSubscriber(), msg.getSubscriptionClassifier());
				})
				.match(UnsubscribeMessage.class, msg -> {
					log.info("Unsubscribe from: "+msg.getSubscriber().path().toString());
					if (msg.getSubscriptionClassifier() == null) {
						oeb.unsubscribe(msg.getSubscriber());
					} else {
						oeb.unsubscribe(msg.getSubscriber(), msg.getSubscriptionClassifier());
					}
				}) 
				.match(OrderEvent.class, event -> {
					log.debug("Received Publish Event: "+event.toString() );
					try {
						tracer.startConsumerSpan(event, "Order Event Bus Wrapper: Order Event received");								
						oeb.publish(event);					
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						tracer.finishCurrentSpan();
					}					
				})
		.build();
	}
}
