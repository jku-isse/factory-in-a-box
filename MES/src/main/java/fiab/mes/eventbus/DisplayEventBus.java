package fiab.mes.eventbus;

import akka.actor.ActorRef;
import akka.event.japi.ScanningEventBus;
import fiab.core.capabilities.events.TimedEvent;
import fiab.mes.assembly.display.msg.DisplayRequest;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.order.msg.RegisterProcessRequest;

public class DisplayEventBus extends ScanningEventBus<DisplayRequest, ActorRef, MESSubscriptionClassifier> {


        @Override
        public void publish(DisplayRequest event, ActorRef subscriber) {
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
        public boolean matches(MESSubscriptionClassifier classifier, DisplayRequest event) {
            if (classifier.eventSource.equals(event.getSender().path().name()))
                return false; // we don't notify sender of event
            if (classifier.topic.equals("*"))
                return true;
            else
                return classifier.topic.equals(event.getSender().path().name());
        }
}
