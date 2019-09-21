package fiab.mes.eventbus;

import akka.actor.ActorRef;

public class UnsubscribeMessage {
	private String topic;
	private ActorRef subscriber;
	private boolean unsubscribeActor;
	
	public UnsubscribeMessage(ActorRef subscriber, String topic) {
		this.topic = topic;
		this.subscriber = subscriber;
		if(topic.equals("")) {
			unsubscribeActor = true;
		} else unsubscribeActor = false;
	}
	
	public boolean getUnsubscribeActor() {
		return unsubscribeActor;
	}

	public String getTopic() {
		return topic;
	}

	public ActorRef getSubscriber() {
		return subscriber;
	}
	

}
