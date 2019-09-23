package fiab.mes.eventbus;
import akka.actor.ActorRef;

public class SubscribeMessage {
	
	private SubscriptionClassifier classifier;
	private ActorRef subscriber;
	
	public SubscribeMessage(ActorRef subscriber, SubscriptionClassifier topic) {
		this.classifier = topic;
		this.subscriber = subscriber;
	}

	public SubscriptionClassifier getSubscriptionClassifier() {
		return classifier;
	}

	public ActorRef getSubscriber() {
		return subscriber;
	}
	
	

}
