package fiab.mes.transport;

import akka.actor.ActorRef;
import akka.event.japi.ScanningEventBus;
import fiab.mes.machine.msg.MachineEvent;

public class MachineLevelEventBus extends ScanningEventBus<MachineEvent, ActorRef, String> {

	
	@Override
	public void publish(MachineEvent event, ActorRef subscriber) {
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
	public boolean matches(String classifier, MachineEvent event) {
		if (classifier.equals("*"))
			return true;
		else 
			return classifier.equals(event.getMachineId());
	}


}