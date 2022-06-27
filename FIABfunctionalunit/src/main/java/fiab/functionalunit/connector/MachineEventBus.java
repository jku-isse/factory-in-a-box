package fiab.functionalunit.connector;

import akka.actor.ActorRef;
import akka.event.japi.ScanningEventBus;
import fiab.core.capabilities.basicmachine.events.MachineEvent;

public class MachineEventBus extends ScanningEventBus<MachineEvent, ActorRef, FUSubscriptionClassifier> {

    @Override
    public void publish(MachineEvent event, ActorRef subscriber) {
        subscriber.tell(event, ActorRef.noSender());
    }

    @Override
    public int compareSubscribers(ActorRef a, ActorRef b) {
        return a.compareTo(b);
    }

    @Override
    public int compareClassifiers(FUSubscriptionClassifier a, FUSubscriptionClassifier b) {
        return a.getTopic().compareTo(b.getTopic());

    }

    @Override
    public boolean matches(FUSubscriptionClassifier classifier, MachineEvent event) {
        if (classifier.getEventSource().equals(event.getMachineId()))
            return false; // we dont notify sender of event
        if (classifier.getTopic().equals("*"))
            return true;
        else
            return classifier.getTopic().equals(event.getMachineId());
    }
}
