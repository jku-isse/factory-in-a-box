package fiab.machine.iostation.opcua;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

public class StartupUtil {

	public static void startupInputstation(int portOffset, String name) {
		startup(portOffset, name, true, true);
	}

	public static void startupInputStationNoAutoReload(int portOffset, String name){
		startup(portOffset, name, true, false);
	}
	
	public static void startupOutputstation(int portOffset, String name) {
		startup(portOffset, name, false, true);
	}
	
	private static void startup(int portOffset, String name, boolean isInputStation, boolean doAutoReload) {
		ActorSystem system = ActorSystem.create("SYSTEM_"+name);		
		ActorRef actor = isInputStation ? system.actorOf(OPCUAIOStationRootActor.propsForInputStation(name, portOffset, doAutoReload), name) :
										system.actorOf(OPCUAIOStationRootActor.propsForOutputStation(name, portOffset, doAutoReload), name);
	}
}
