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
		ActorRef actor = isInputStation ? system.actorOf(OPCUAIOStationRootActor.propsForInputStation(name, portOffset, doAutoReload, false), name) :
										system.actorOf(OPCUAIOStationRootActor.propsForOutputStation(name, portOffset, doAutoReload, false), name);
	}

	private static void startupEV3(int portOffset, String name, boolean isInputStation){
		ActorSystem system = ActorSystem.create("SYSTEM_"+name);
		ActorRef actor = isInputStation ? system.actorOf(OPCUAIOStationRootActor.propsForInputStation(name, portOffset, false, true), name) :
				system.actorOf(OPCUAIOStationRootActor.propsForOutputStation(name, portOffset, false, true), name);
	}

	public static void startupEV3Inputstation(int portOffset, String name) {
		startupEV3(portOffset, name, true);
	}

	public static void startupEV3Outputstation(int portOffset, String name) {
		startupEV3(portOffset, name, false);
	}

	public static void startupEV3InputStationUsingActorSystem(ActorSystem system, int portOffset, String name){
		system.actorOf(OPCUAIOStationRootActor.propsForInputStation(name, portOffset, false, true), name);
	}

	public static void startupEV3OutputStationUsingActorSystem(ActorSystem system, int portOffset, String name){
		system.actorOf(OPCUAIOStationRootActor.propsForOutputStation(name, portOffset, false, true), name);
	}
}
