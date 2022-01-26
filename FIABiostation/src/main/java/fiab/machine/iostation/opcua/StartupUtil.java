package fiab.machine.iostation.opcua;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import ev3dev.sensors.ev3.EV3ColorSensor;
import lejos.hardware.port.SensorPort;

public class StartupUtil {

	private static EV3ColorSensor colorSensor;

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
		ActorRef actor = isInputStation ? system.actorOf(OPCUAIOStationRootActor.propsForInputStation(name, portOffset, colorSensor, doAutoReload, false), name) :
										system.actorOf(OPCUAIOStationRootActor.propsForOutputStation(name, portOffset, colorSensor, doAutoReload, false), name);
	}

	private static void startupEV3(int portOffset, String name, boolean isInputStation){
		ActorSystem system = ActorSystem.create("SYSTEM_"+name);
		ActorRef actor = isInputStation ? system.actorOf(OPCUAIOStationRootActor.propsForInputStation(name, portOffset, colorSensor, false, true), name) :
				system.actorOf(OPCUAIOStationRootActor.propsForOutputStation(name, portOffset, colorSensor, false, false), name);
	}

	public static void startupEV3Inputstation(int portOffset, String name) {
		startupEV3(portOffset, name, true);
	}

	public static void startupEV3Outputstation(int portOffset, String name) {
		startupEV3(portOffset, name, false);
	}

	public static void startupEV3InputStationUsingActorSystem(ActorSystem system, int portOffset, String name){
		if(colorSensor == null){
			System.out.println("ColorSensor is null, creating new one at sensorPort1");
			colorSensor = new EV3ColorSensor(SensorPort.S1);
			System.out.println("ColorSensor is " + colorSensor);
		}
		system.actorOf(OPCUAIOStationRootActor.propsForInputStation(name, portOffset, colorSensor, true, true), name);
	}

	public static void startupEV3OutputStationUsingActorSystem(ActorSystem system, int portOffset, String name){
		if(colorSensor == null){
			System.out.println("ColorSensor is null, creating new one at sensorPort1");
			colorSensor = new EV3ColorSensor(SensorPort.S1);
			System.out.println("ColorSensor is " + colorSensor);
		}
		system.actorOf(OPCUAIOStationRootActor.propsForOutputStation(name, portOffset, colorSensor, false, false), name);
	}
}
