package TestPurposes;

import akka.actor.AbstractActor;
import akka.actor.ActorSystem;
import akka.actor.Props;
import fiab.mes.machine.msg.MachineUpdateEvent;
import fiab.mes.transport.actor.turntable.TransportModuleActor;

public class SimpleActor extends AbstractActor {

	public SimpleActor(String name) {

	}
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(MachineUpdateEvent.class, msg -> {
					System.out.println("We got smth");
				})
				.build();
	}
	public static Props props(String name) {
		return Props.create(SimpleActor.class, () -> new SimpleActor(name));
	}

}
