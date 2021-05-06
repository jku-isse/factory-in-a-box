package fiab.machine.iostation.opcua;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import fiab.tracing.extension.TracingExtension;

public class StartupUtil {
//TracingExtension can simply be null, if there is now certain extension used
	public static void startupInputstation(int portOffset, String name, TracingExtension extension) {
		startup(portOffset, name, true, extension);
	}

	public static void startupOutputstation(int portOffset, String name, TracingExtension extension) {
		startup(portOffset, name, false, extension);
	}

	private static void startup(int portOffset, String name, boolean isInputStation, TracingExtension extension) {
		ActorSystem system = ActorSystem.create("SYSTEM_" + name);
		if(extension !=null)
		system.registerExtension(extension);
		ActorRef actor = isInputStation
				? system.actorOf(OPCUAIOStationRootActor.propsForInputStation(name, portOffset), name)
				: system.actorOf(OPCUAIOStationRootActor.propsForOutputStation(name, portOffset), name);
	}
}
