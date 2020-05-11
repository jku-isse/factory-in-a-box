package fiab.mes.mockactors.transport.opcua;

import fiab.core.capabilities.plotting.WellknownPlotterCapability.SupportedColors;
import fiab.machine.plotter.opcua.StartupUtil;

public class StartupVirtualPlotter {

	public static void main(String[] args) {
		StartupUtil.startup(5, "VirtualPlotter1", SupportedColors.BLACK);
	}

}
