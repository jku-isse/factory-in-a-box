package fiab.mes.transport.actor.turntable;

import java.util.HashMap;
import java.util.Map;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import fiab.mes.machine.msg.MachineStatusUpdateEvent;
import fiab.mes.machine.msg.MachineUpdateEvent;
import fiab.mes.transport.actor.wrapper.ConveyorWrapper;

public class ConveyorActor extends AbstractActor{
	private ConveyorWrapper wrapper;
	private Map<String, String> serverStates = new HashMap<String, String>();
	private String id;
	private ActorRef boss;


	/*
	 * Got a ProcessActor
	 */

	public ConveyorActor(ActorRef boss, ConveyorWrapper wrapper) {
		this.wrapper = wrapper;
	}

	public static Props props(ActorRef boss, ConveyorWrapper wrapper) {
		return Props.create(ConveyorActor.class, () -> new ConveyorActor(boss, wrapper));
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
		}).match(MachineStatusUpdateEvent.class, msg -> {
			serverStates.replace(msg.getNodeId(), msg.getStatus().toString());
		})
		.build();
	}

}
