package fiab.machine.iostation.opcua;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import fiab.tracing.util.Util;

public class StartupUtil {

	public static void startupInputstation(int portOffset, String name) {
		startup(portOffset, name, true);
	}
	
	public static void startupOutputstation(int portOffset, String name) {
		startup(portOffset, name, false);
	}
	
	private static void startup(int portOffset, String name, boolean isInputStation) {
		ActorSystem system = ActorSystem.create("SYSTEM_"+name);	
		system.registerExtension(Util.getTracingExtension());
		ActorRef actor = isInputStation ? system.actorOf(OPCUAIOStationRootActor.propsForInputStation(name, portOffset), name) :
										system.actorOf(OPCUAIOStationRootActor.propsForOutputStation(name, portOffset), name);
	}
}
