package fiab.machine.plotter.opcua;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import fiab.core.capabilities.plotting.WellknownPlotterCapability.SupportedColors;
import fiab.tracing.extension.TracingExtension;

public class StartupUtil {

	
	
	public static void startup(int portOffset, String name, SupportedColors color,TracingExtension ext) {
		ActorSystem system = ActorSystem.create("SYSTEM_"+name);
		system.registerExtension(ext);
		ActorRef actor = system.actorOf(OPCUAPlotterRootActor.props(name, portOffset, color), name);
	}
}
