package shopfloor.agents.eventbus;

import akka.actor.ActorRef;
import akka.event.japi.ScanningEventBus;
import shopfloor.agents.events.OrderBaseEvent;

public class OrderEventBus  extends ScanningEventBus<OrderBaseEvent, ActorRef, String>{

	

	@Override
	public void publish(OrderBaseEvent event, ActorRef subscriber) {
		subscriber.tell(event, ActorRef.noSender());
		
	}

	@Override
	public int compareClassifiers(String a, String b) {
		return a.compareTo(b);
	}

	@Override
	public int compareSubscribers(ActorRef a, ActorRef b) {
		return a.compareTo(b);
	}

	@Override
	public boolean matches(String classifier, OrderBaseEvent event) {
		if (classifier.equals("*"))
			return true;
		else 
			return classifier.equals(event.getOrderId());
	}

}
