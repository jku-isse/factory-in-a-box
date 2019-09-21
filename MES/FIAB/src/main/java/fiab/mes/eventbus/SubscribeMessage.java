package fiab.mes.eventbus;
import akka.actor.ActorRef;

public class SubscribeMessage {
	
	private String topic;
	private ActorRef subscriber;
	
	public SubscribeMessage(ActorRef subscriber, String topic) {
		this.topic = topic;
		this.subscriber = subscriber;
	}

	public String getTopic() {
		return topic;
	}

	public ActorRef getSubscriber() {
		return subscriber;
	}
	
	

}
