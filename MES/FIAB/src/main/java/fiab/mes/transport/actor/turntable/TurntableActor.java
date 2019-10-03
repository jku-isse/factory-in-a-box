package fiab.mes.transport.actor.turntable;

import java.util.HashMap;
import java.util.Map;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import fiab.mes.machine.msg.MachineUpdateEvent;
import fiab.mes.transport.actor.wrapper.TurntableWrapper;

public class TurntableActor extends AbstractActor {
	private TurntableWrapper wrapper;
	private Map<String, String> serverStates = new HashMap<String, String>();
	private String id;
	private ActorRef boss;

	/*
	 * Got a ProcessActor I guess
	 */

	public TurntableActor(ActorRef boss, TurntableWrapper wrapper) {
		this.wrapper = wrapper;
	}

	public static Props props(ActorRef boss, TurntableWrapper wrapper) {
		return Props.create(TurntableActor.class, () -> new TurntableActor(boss, wrapper));
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(String.class, string -> { // TODO add or change responses
			if (string.equals("GET_NAME")) {
				sender().tell("Name:" + id, getSelf());
			} else if (string.startsWith("Subscribe ")) {
				System.out.println("Subscribing to: " + string);
				wrapper.subscribe(getSelf(), string.substring(10));
				serverStates.put(string.substring(10), "");
			}
		}).match(MachineUpdateEvent.class, msg -> {
			serverStates.replace(msg.getNodeId(), msg.getNewValue().toString());
		})
		.build();
	}
}
