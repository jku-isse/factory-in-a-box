package shopfloor.agents;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import shopfloor.agents.impl.MachineAgent;
import shopfloor.agents.impl.OrderAgent;
import shopfloor.agents.messages.OrderDocument;
import shopfloor.agents.messages.ProductionStateUpdate;
import shopfloor.agents.messages.ProductionStateUpdate.ProductionState;

public class ShopfloorControl {

	public static void main(String[] args) {
		final ActorSystem system = ActorSystem.create("helloakka");
		try {
			final ActorRef m1 = system.actorOf(MachineAgent.props("Machine1"));
			final ActorRef m2 = system.actorOf(MachineAgent.props("Machine2"));
			ArrayList<String> jobs = new ArrayList<>();
			jobs.add("DrawTree");
			jobs.add("DrawSun");
			jobs.add("DrawLake");
			HashMap<String, ActorRef> job2machineDict = new HashMap<>();
			job2machineDict.put("DrawTree", m1);
			job2machineDict.put("DrawSun", m2);
			job2machineDict.put("DrawLake", m1);
			OrderDocument order1 = new OrderDocument("Order1", jobs);
			final ActorRef orderActor = system.actorOf(OrderAgent.props(order1, job2machineDict));			
			orderActor.tell(new ProductionStateUpdate(ProductionState.COMPLETED, "INITJOB", Instant.now()), ActorRef.noSender());

			System.out.println(">>> Press ENTER to exit <<<");
			System.in.read();
		} catch (IOException ioe) {
		} finally {
			system.terminate();
		}
	}

}
