package fiab.opcua.hardwaremock.plotter;

import akka.actor.ActorSystem;
import fiab.mes.capabilities.plotting.WellknownPlotterCapability.SupportedColors;

public class StartupPlotter1OPCUAMock {	

	public static void main(String[] args) throws Exception {
		ActorSystem system = ActorSystem.create("ROOT_SYSTEM_PLOTTER1_OPCUA");
		system.actorOf(OPCUAPlotterRootActor.props("Plotter1", SupportedColors.BLACK));
	}
	
	
}
