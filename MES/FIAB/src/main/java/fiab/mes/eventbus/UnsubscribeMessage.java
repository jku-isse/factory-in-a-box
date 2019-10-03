package fiab.mes.eventbus;

import akka.actor.ActorRef;

public class UnsubscribeMessage {
	private SubscriptionClassifier topic;
	private ActorRef subscriber;
	
	public UnsubscribeMessage(ActorRef subscriber, SubscriptionClassifier topic) {
		this.topic = topic;
		this.subscriber = subscriber;
	}
	
	public SubscriptionClassifier getSubscriptionClassifier() {
		return topic;
	}

	public ActorRef getSubscriber() {
		return subscriber;
	}
	

}
