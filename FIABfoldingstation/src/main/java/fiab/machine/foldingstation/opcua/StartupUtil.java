package fiab.machine.foldingstation.opcua;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import fiab.core.capabilities.folding.WellknownFoldingCapability;

public class StartupUtil {
    public static void startup(int portOffset, String name) {
        ActorSystem system = ActorSystem.create("SYSTEM_"+name);
        ActorRef actor = system.actorOf(OPCUAFoldingStationRootActor.props(name, portOffset), name);
    }
}
