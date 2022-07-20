package fiab.core.capabilities.plotting;

import ProcessCore.AbstractCapability;
import ProcessCore.Parameter;
import ProcessCore.ProcessCoreFactory;
import fiab.core.capabilities.ComparableCapability;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.functionalunit.BasicFUBehaviour;

public interface WellknownPlotterCapability extends BasicFUBehaviour, OPCUABasicMachineBrowsenames {

	public static String PLOTTING_CAPABILITY_BASE_URI = "http://factory-in-a-box.fiab/capabilities/plot/color/";

	public static String OPCUA_PLOT_REQUEST = "PLOT";
	String OPCUA_SET_CAPABILITY = "SET_CAPABILITY";
	
	public static enum SupportedColors { //default static java.awt.Color
		BLACK, BLUE, GREEN, RED, CYAN, GRAY, DARK_GRAY, LIGHT_GRAY, MAGENTA, ORANGE, PINK, WHITE, YELLOW
	}
	
	
	static AbstractCapability getColorPlottingCapability(SupportedColors color) {		
		ComparableCapability ac = new ComparableCapability();
		ac.setDisplayName("Plot "+color);
		ac.setID("Capability.Plotting.Color."+color);
		ac.setUri(generatePlottingCapabilityURI(color));
		ac.getInputs().add(getImageInputParameter());
		return ac;
	}
	
	public static String generatePlottingCapabilityURI(SupportedColors color) {
		return PLOTTING_CAPABILITY_BASE_URI+color;
	}
	
	public static String PLOTTING_CAPABILITY_INPUT_IMAGE_VAR_NAME = "RD_1";
	
	static Parameter getImageInputParameter() {
		Parameter inImage = ProcessCoreFactory.eINSTANCE.createParameter();
		inImage.setName(PLOTTING_CAPABILITY_INPUT_IMAGE_VAR_NAME);
		inImage.setType("String");
		return inImage;
	}
	
}
