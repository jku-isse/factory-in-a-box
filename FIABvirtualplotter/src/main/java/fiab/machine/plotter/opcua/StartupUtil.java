package fiab.machine.plotter.opcua;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import fiab.core.capabilities.plotting.WellknownPlotterCapability.SupportedColors;
import fiab.tracing.util.Util;

public class StartupUtil {

	
	
	public static void startup(int portOffset, String name, SupportedColors color) {
		ActorSystem system = ActorSystem.create("SYSTEM_"+name);
		system.registerExtension(Util.getTracingExtension());
		ActorRef actor = system.actorOf(OPCUAPlotterRootActor.props(name, portOffset, color), name);
	}
}
