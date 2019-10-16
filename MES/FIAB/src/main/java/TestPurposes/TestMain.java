package TestPurposes;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import fiab.mes.machine.msg.MachineEvent;
import fiab.mes.machine.msg.MachineUpdateEvent;
import fiab.mes.transport.MachineLevelEventBus;
import fiab.mes.transport.actor.turntable.TransportModuleActor;

public class TestMain {
	
	private static ActorRef tma;
	
	public static void main(String[] args) throws InterruptedException {
		MachineLevelEventBus bus = new MachineLevelEventBus();
		ActorSystem system = ActorSystem.create("Test");
		ActorRef simpleActor = system.actorOf(SimpleActor.props("Simplix"));
		tma = system.actorOf(TransportModuleActor.props("testaddress", system), "TMA");
		bus.subscribe(simpleActor, "*");
		bus.subscribe(tma, "*");
//		bus.publish(new MachineUpdateEvent("MACHINEID", "NODEID", MachineEvent.MachineEventType.UPDATE, "hi"));
//		bus.publish(new MachineEvent("NEW MACHINE", MachineEvent.MachineEventType.UPDATE));
		
		
		
		for(int i = 0; i < 2; i++) {
			Thread.sleep(1000);
			System.out.print(".");
		}
		System.out.println();
		tma.tell("TRANSPORT", ActorRef.noSender());
		
	}

}
