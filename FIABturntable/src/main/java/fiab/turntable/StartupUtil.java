package fiab.turntable;

import akka.actor.ActorSystem;

public class StartupUtil {
	public static void startupWithExposedInternalControls(int portOffset, String name) {
		startup(portOffset, name, true);
	}
	
	public static void startupWithHiddenInternalControls(int portOffset, String name) {
		startup(portOffset, name, false);
	}
	
	private static void startup(int portOffset, String name, boolean exposeInternalControls) {
		ActorSystem system = ActorSystem.create("SYSTEM_"+name);		
        //system.actorOf(OPCUATurntableRootActor.props(name, portOffset, exposeInternalControls), "TurntableRoot");
        }
}
