package fiab.mes.shopfloor;

import main.java.fiab.core.capabilities.plotting.WellknownPlotterCapability.SupportedColors;
import fiab.machine.plotter.opcua.StartupUtil;

public class StartupVirtualOPCUAparticipants {

	// Checkout for Port to Position mapping:
			//TransportPositionLookup.parsePosViaPortNr();
			// Checkout Position arrangement
			//new HardcodedDefaultTransportRoutingAndMapping
	
	public static void main(String[] args) {
		startupSingleTurntableInputOutputDualPlotter();
		
	}
	
	public static void startupSingleTurntableInputOutputDualPlotter() {
		// TT1 West
		fiab.machine.iostation.opcua.StartupUtil.startupInputstation(0, "InputStation1");
		// TT1 North
		StartupUtil.startup(5, "VirtualPlotter1", SupportedColors.BLACK);
		// TT1 South
		StartupUtil.startup(7, "VirtualPlotter2", SupportedColors.BLACK);
		// TT1 EAST
		fiab.machine.iostation.opcua.StartupUtil.startupOutputstation(3, "OutputStation1");
		// TT1 itself - ensure this starts later than the others or has no prior wiring configured
		fiab.turntable.StartupUtil.startupWithHiddenInternalControls(2, "Turntable1");
		
	}
}
