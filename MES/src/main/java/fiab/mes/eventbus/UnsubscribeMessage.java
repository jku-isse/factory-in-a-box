package fiab.mes.eventbus;

import akka.actor.ActorRef;

public class UnsubscribeMessage {
	private MESSubscriptionClassifier topic;
	private ActorRef subscriber;
	
	public UnsubscribeMessage(ActorRef subscriber, MESSubscriptionClassifier topic) {
		this.topic = topic;
		this.subscriber = subscriber;
	}
	
	public MESSubscriptionClassifier getSubscriptionClassifier() {
		return topic;
	}

	public ActorRef getSubscriber() {
		return subscriber;
	}
	

}
