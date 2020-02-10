package fiab.mes.eventbus;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.mes.general.TimedEvent;
import fiab.mes.machine.msg.GenericMachineRequests;
import fiab.mes.machine.msg.MachineEvent;
import fiab.mes.order.msg.OrderEvent;

public class InterMachineEventBusWrapperActor extends AbstractActor {

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	public static final String WRAPPER_ACTOR_LOOKUP_NAME = "InterMachineEventBusWrapperActor";
	private InterMachineEventBus meb;

	public InterMachineEventBusWrapperActor() {
		meb = new InterMachineEventBus();
	}

	public static Props props() {
		return Props.create(InterMachineEventBusWrapperActor.class, () -> new InterMachineEventBusWrapperActor());
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(SubscribeMessage.class, msg -> {
					log.info("Subscribe from: "+msg.getSubscriber().path().toString());
					meb.subscribe(msg.getSubscriber(), msg.getSubscriptionClassifier());
				})
				.match(UnsubscribeMessage.class, msg -> {
					log.info("Unsubscribe from: "+msg.getSubscriber().path().toString());
					if (msg.getSubscriptionClassifier() == null) {
						meb.unsubscribe(msg.getSubscriber());
					} else {
						meb.unsubscribe(msg.getSubscriber(), msg.getSubscriptionClassifier());
					}
				}) 
				.match(MachineEvent.class, e -> {
					log.debug("Received Publish Event: "+e.toString() );
					meb.publish(e);
				})
		.build();
	}

}
