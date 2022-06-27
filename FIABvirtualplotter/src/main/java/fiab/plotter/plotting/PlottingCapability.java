package fiab.plotter.plotting;

import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.functionalunit.BasicFUBehaviour;

public interface PlottingCapability extends BasicFUBehaviour, OPCUABasicMachineBrowsenames {

    String CAPABILITY_ID = "DefaultPlottingCapability";

    String OPC_UA_BASE_URI = "http://factory-in-a-box.fiab/capabilities/plot/color/";

    String PLOT_REQUEST = "PLOT";

    String PlOTTING_ID = "PlotterFU";

    void plot(String imageId, String orderId);
}
