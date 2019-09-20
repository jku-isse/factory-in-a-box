package fiab.mes.eventbus;

import akka.actor.AbstractActor;
import akka.actor.Props;
import fiab.mes.general.TimedEvent;
import fiab.mes.transport.msg.MachineEvent;
import fiab.mes.transport.msg.SubscribeMessage;
import fiab.mes.transport.msg.UnsubscribeMessage;

public class HLEB_WrapperActor extends AbstractActor {

	private HighLevelEventBus hleb;

	public HLEB_WrapperActor() {
		hleb = new HighLevelEventBus();
	}

	public static Props props() {
		return Props.create(HLEB_WrapperActor.class, () -> new HLEB_WrapperActor());
	}

	@SuppressWarnings("unchecked")
	@Override
	public Receive createReceive() {
		return receiveBuilder()
		.match(SubscribeMessage.class, msg -> {
			hleb.subscribe(msg.getSubscriber(), msg.getTopic());
		})
		.match(UnsubscribeMessage.class, msg -> {
			if (msg.getUnsubscribeActor()) {
				hleb.unsubscribe(msg.getSubscriber());
			} else {
				hleb.unsubscribe(msg.getSubscriber(), msg.getTopic());
			}
		})
		.matchAny(msg -> {
			if(msg instanceof TimedEvent) {
				hleb.publish((Class<? extends TimedEvent>) msg);
			}
		})
		
		
		.build();
	}

}
