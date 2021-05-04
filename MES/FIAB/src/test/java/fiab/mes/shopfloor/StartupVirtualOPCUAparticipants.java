package fiab.mes.shopfloor;

import fiab.core.capabilities.plotting.WellknownPlotterCapability.SupportedColors;
import fiab.core.capabilities.tracing.TestTracingUtil;
import fiab.machine.plotter.opcua.StartupUtil;

import fiab.tracing.extension.TracingExtension;

public class StartupVirtualOPCUAparticipants {

	// Checkout for Port to Position mapping:
	// TransportPositionLookup.parsePosViaPortNr();
	// Checkout Position arrangement
	// new HardcodedDefaultTransportRoutingAndMapping

	public static void main(String[] args) {
		startupDoubleTurntableInputOutput();
	}

	public static void startupSingleTurntableInputOutputDualPlotter() {
		TracingExtension ext = TestTracingUtil.getTracingExtension();
		// TT1 West
		fiab.machine.iostation.opcua.StartupUtil.startupInputstation(0, "InputStation1", ext);
		// TT1 North
		StartupUtil.startup(5, "VirtualPlotter1", SupportedColors.BLACK, ext);
		// TT1 South
		StartupUtil.startup(7, "VirtualPlotter2", SupportedColors.BLACK, ext);
		// TT1 EAST
		fiab.machine.iostation.opcua.StartupUtil.startupOutputstation(3, "OutputStation1", ext);
		// TT1 itself - ensure this starts later than the others or has no prior wiring
		// configured
		fiab.turntable.StartupUtil.startupWithHiddenInternalControls(2, "Turntable1TracingTest", ext);

	}

	public static void startupDoubleTurntableInputOutput() {
		TracingExtension ext = TestTracingUtil.getTracingExtension();
		// TT1 West
		fiab.machine.iostation.opcua.StartupUtil.startupInputstation(0, "InputStation1", ext);
		// TT1 EAST
		fiab.machine.iostation.opcua.StartupUtil.startupOutputstation(7, "OutputStation1", ext);
		// TT1 itself - ensure this starts later than the others or has no prior wiring
		// configured
		fiab.turntable.StartupUtil.startupWithHiddenInternalControls(2, "Turntable1DualTTWiring", ext);
		fiab.turntable.StartupUtil.startupWithHiddenInternalControls(3, "Turntable2DualTTWiring", ext);
	}
}
