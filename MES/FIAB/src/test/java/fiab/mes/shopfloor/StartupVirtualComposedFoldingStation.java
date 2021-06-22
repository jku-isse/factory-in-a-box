package fiab.mes.shopfloor;

import fiab.core.capabilities.folding.WellknownFoldingCapability;
import fiab.machine.foldingstation.opcua.StartupUtil;

public class StartupVirtualComposedFoldingStation {

    public static void main(String[] args) {
        startupSingleTurntableInputOutputDualFolding();

    }

    public static void startupSingleTurntableInputOutputDualFolding() {
        // TT1 West
        fiab.machine.iostation.opcua.StartupUtil.startupInputstation(0, "InputStation1");
        // TT1 North
        StartupUtil.startup(5, "VirtualFolding1", WellknownFoldingCapability.SupportedShapes.BOX);
        // TT1 South
        StartupUtil.startup(7, "VirtualFolding2", WellknownFoldingCapability.SupportedShapes.BOX);
        // TT1 EAST
        fiab.machine.iostation.opcua.StartupUtil.startupOutputstation(3, "OutputStation1");
        // TT1 itself - ensure this starts later than the others or has no prior wiring configured
        fiab.turntable.StartupUtil.startupWithHiddenInternalControls(2, "Turntable1");

    }
}
