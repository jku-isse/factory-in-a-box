package fiab.mes.eventbus;

import akka.actor.ActorRef;
import akka.event.japi.ScanningEventBus;
import fiab.mes.order.msg.OrderEvent;

public class OrderEventBus extends ScanningEventBus<OrderEvent, ActorRef, MESSubscriptionClassifier>{
	

	@Override
	public void publish(OrderEvent event, ActorRef subscriber) {
		subscriber.tell(event, ActorRef.noSender());
		
	}

	@Override
	public int compareSubscribers(ActorRef a, ActorRef b) {
		return a.compareTo(b);
	}

	@Override
	public int compareClassifiers(MESSubscriptionClassifier a, MESSubscriptionClassifier b) {
		return a.topic.compareTo(b.topic);
		
	}
	
	@Override
	public boolean matches(MESSubscriptionClassifier classifier, OrderEvent event) {
		if (classifier.eventSource.equals(event.getMachineId()))
			return false; // we dont notify sender of event
		if (classifier.topic.equals("*"))
			return true;
		else 
			return classifier.topic.equals(event.getOrderId());
	}




	
}