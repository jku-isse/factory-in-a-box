package fiab.mes.eventbus;

import akka.actor.ActorRef;
import akka.event.japi.ScanningEventBus;
import main.java.fiab.core.capabilities.basicmachine.events.MachineEvent;

public class InterMachineEventBus extends ScanningEventBus<MachineEvent, ActorRef, MESSubscriptionClassifier> {


	@Override
	public void publish(MachineEvent event, ActorRef subscriber) {
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
	public boolean matches(MESSubscriptionClassifier classifier, MachineEvent event) {
		if (classifier.eventSource.equals(event.getMachineId()))
			return false; // we dont notify sender of event
		if (classifier.topic.equals("*"))
			return true;
		else 
			return classifier.topic.equals(event.getMachineId());
	}
}