package fiab.mes.eventbus;

import akka.actor.ActorRef;
import akka.event.japi.ScanningEventBus;
import fiab.mes.order.msg.OrderEvent;

public class OrderEventBus  extends ScanningEventBus<OrderEvent, ActorRef, String>{
	

	@Override
	public void publish(OrderEvent event, ActorRef subscriber) {
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
	public boolean matches(String classifier, OrderEvent event) {
		if (classifier.equals("*"))
			return true;
		else 
			return classifier.equals(event.getOrderId());
	}

}