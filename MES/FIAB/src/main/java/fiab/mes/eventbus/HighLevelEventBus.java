package fiab.mes.eventbus;

import akka.actor.ActorRef;
import akka.event.japi.ScanningEventBus;
import fiab.mes.general.TimedEvent;
import fiab.mes.machine.msg.MachineEvent;
import fiab.mes.order.msg.OrderEvent;

public class HighLevelEventBus extends ScanningEventBus<Class<? extends MachineEvent>, ActorRef, String> {

	
	@Override
	public void publish(Class<? extends MachineEvent> event, ActorRef subscriber) {
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

	private <T extends MachineEvent> boolean matches(String classifier, T event) {
		if (classifier.equals("*"))
			return true;
		else {
			return classifier.equals(event.getMachineId());
		}
	}

	@Override
	public boolean matches(String classifier, Class<? extends MachineEvent> event) {
		return matches(classifier, event.cast(event.getClass()));
	}

}