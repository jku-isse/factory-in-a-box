package fiab.machine.plotter.opcua;

import fiab.core.capabilities.plotting.WellknownPlotterCapability.SupportedColors;

public class ExampleStartupPlotter {

	public static void main(String[] args) {
		StartupUtil.startup(0, "TestPlotter", SupportedColors.BLACK);
	}

}
