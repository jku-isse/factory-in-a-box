package fiab.mes.machine.actor.plotter;

import ProcessCore.AbstractCapability;
import ProcessCore.Parameter;
import ProcessCore.ProcessCoreFactory;
import fiab.mes.general.ComparableCapability;

public interface WellknownPlotterCapability {

	//TODO: include the colors for plotting
	public static String PLOTTING_CAPABILITY_URI = "http://factory-in-a-box.fiab/capabilities/plot";

	public static String MACHINE_UPCUA_PLOT_REQUEST = "PLOT";
	
	static AbstractCapability getPlottingCapability() {
		ComparableCapability ac = new ComparableCapability();
		ac.setDisplayName("Plot");
		ac.setID("Capability.Plotting");
		ac.setID(PLOTTING_CAPABILITY_URI);		
		return ac;
	}
	
	public static String PLOTTING_CAPABILITY_INPUT_IMAGE_VAR_NAME = "RD_1";
	
	static Parameter getImageInputParameter() {
		Parameter inImage = ProcessCoreFactory.eINSTANCE.createParameter();
		inImage.setName(PLOTTING_CAPABILITY_INPUT_IMAGE_VAR_NAME);
		inImage.setType("String");
		return inImage;
	}
	
}
