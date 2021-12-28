package fiab.machine.foldingstation.opcua;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import fiab.core.capabilities.folding.WellknownFoldingCapability;
import lejos.hardware.port.SensorPort;

public class StartupUtil {
    static ActorSystem system;

    public static void startup(int portOffset, String name) {
        system = ActorSystem.create("SYSTEM_" + name);
        ActorRef actor = system.actorOf(OPCUAFoldingStationRootActor.props(name, portOffset, null), name);
    }

    public static void startAnother(int portOffset, String name) {
        if(system != null) {
            ActorRef actorRef = system.actorOf(OPCUAFoldingStationRootActor.props(name, portOffset, null), name);
        }else{
            throw new RuntimeException("ActorSystem is not online, make sure startup in FoldingStation has been called");
        }
    }

    public static ActorSystem startupEV3(int portOffset, String name) {
        system = ActorSystem.create("SYSTEM_" + name);
        ActorRef actor = system.actorOf(OPCUAFoldingStationRootActor.props(name, portOffset, SensorPort.S3), name);
        return system;
    }

    public static void startAnotherEV3(int portOffset, String name) {
        if(system != null) {
            ActorRef actorRef = system.actorOf(OPCUAFoldingStationRootActor.props(name, portOffset, SensorPort.S4), name);
        }else{
            throw new RuntimeException("ActorSystem is not online, make sure startup in FoldingStation has been called");
        }
    }
}
