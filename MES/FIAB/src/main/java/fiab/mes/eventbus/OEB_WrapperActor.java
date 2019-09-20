package fiab.mes.eventbus;

import akka.actor.AbstractActor;
import akka.actor.Props;
import fiab.mes.transport.msg.SubscribeMessage;
import fiab.mes.transport.msg.UnsubscribeMessage;

public class OEB_WrapperActor extends AbstractActor {
	
	private OrderEventBus oeb;

	public OEB_WrapperActor() {
		oeb = new OrderEventBus();
	}

	public static Props props() {
		return Props.create(OEB_WrapperActor.class, () -> new OEB_WrapperActor());
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(SubscribeMessage.class, msg -> {
			oeb.subscribe(msg.getSubscriber(), msg.getTopic());
		}).match(UnsubscribeMessage.class, msg -> {
			if (msg.getUnsubscribeActor()) {
				oeb.unsubscribe(msg.getSubscriber());
			} else {
				oeb.unsubscribe(msg.getSubscriber(), msg.getTopic());
			}
		}) //TODO fire events on eventbus
		.build();
	}
}
