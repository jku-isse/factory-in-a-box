package fiab.mes.eventbus;
import akka.actor.ActorRef;

public class SubscribeMessage {
	
	private MESSubscriptionClassifier classifier;
	private ActorRef subscriber;
	
	public SubscribeMessage(ActorRef subscriber, MESSubscriptionClassifier topic) {
		this.classifier = topic;
		this.subscriber = subscriber;
	}

	public MESSubscriptionClassifier getSubscriptionClassifier() {
		return classifier;
	}

	public ActorRef getSubscriber() {
		return subscriber;
	}
	
	

}
