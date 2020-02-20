package fiab.mes.machine.actor.plotter;

import ProcessCore.AbstractCapability;
import fiab.mes.general.ComparableCapability;

public interface WellknownPlotterCapability {

	public static String PLOTTING_CAPABILITY_URI = "http://factory-in-a-box.fiab/capabilities/plot";

	public static String MACHINE_UPCUA_PLOT_REQUEST = "PLOT";
	
	static AbstractCapability getPlottingCapability() {
		ComparableCapability ac = new ComparableCapability();
		ac.setDisplayName("Plot");
		ac.setID("Capability.Plotting");
		ac.setID(PLOTTING_CAPABILITY_URI);
		return ac;
	}
}
