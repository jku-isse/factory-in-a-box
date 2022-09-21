package fiab.mes.eventbus;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.core.capabilities.basicmachine.events.MachineEvent;
import fiab.functionalunit.connector.MachineEventBus;
import fiab.mes.machine.msg.OrderRelocationNotification;

//Switched to MachineEventbus from FIABfunctionalunit to remove code duplication and enable interoperability
public class InterMachineEventBusWrapperActor extends AbstractActor {

	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	public static final String WRAPPER_ACTOR_LOOKUP_NAME = "InterMachineEventBusWrapperActor";
	private final MachineEventBus meb;

	public InterMachineEventBusWrapperActor(MachineEventBus bus) {
		meb = bus == null ? new MachineEventBus() : bus;
	}

	public static Props props() {
		return Props.create(InterMachineEventBusWrapperActor.class, () -> new InterMachineEventBusWrapperActor(null));
	}
	
	public static Props propsWithPreparedBus(MachineEventBus bus) {
		return Props.create(InterMachineEventBusWrapperActor.class, () -> new InterMachineEventBusWrapperActor(bus));
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
				.match(OrderRelocationNotification.class, msg -> {
					log.info("Relocation notification for Order: " + msg.getOrderId());
					meb.publish(msg);
				})
		.build();
	}

}
