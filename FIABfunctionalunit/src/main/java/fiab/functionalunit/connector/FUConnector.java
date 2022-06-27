package fiab.functionalunit.connector;

import akka.actor.ActorRef;
import akka.event.japi.ScanningEventBus;
import fiab.core.capabilities.functionalunit.FURequest;

public class FUConnector extends ScanningEventBus<FURequest, ActorRef, FUSubscriptionClassifier> {

    @Override
    public void publish(FURequest request, ActorRef subscriber) {
        subscriber.tell(request, ActorRef.noSender());
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
    public boolean matches(FUSubscriptionClassifier classifier, FURequest request) {
        if (classifier.getEventSource().equals(request.getSenderId())) {
            return false; // we dont notify sender of event
        } else if (classifier.getTopic().equals("*")) {
            return true;
        } else {
            return classifier.getTopic().equals(request.getSenderId());
        }
    }
}
