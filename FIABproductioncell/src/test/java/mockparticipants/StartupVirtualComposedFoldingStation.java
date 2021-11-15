package mockparticipants;

import fiab.machine.foldingstation.opcua.StartupUtil;

public class StartupVirtualComposedFoldingStation {

    public static void main(String[] args) {
        startupSingleTurntableInputOutputDualFolding();
    }

    public static void startupSingleTurntableInputOutputDualFolding() {
        // TT1 West
        fiab.machine.iostation.opcua.StartupUtil.startupInputstation(0, "InputStation");
        // TT1 North, TT2 North
        StartupUtil.startup(5, "VirtualFolding1");
        // TT1 South, TT2 South
        StartupUtil.startup(7, "VirtualFolding2");
        // TT2 EAST
        fiab.machine.iostation.opcua.StartupUtil.startupOutputstation(10, "OutputStation");
        // TT1 itself - ensure this starts later than the others or has no prior wiring configured
        fiab.turntable.StartupUtil.startupWithHiddenInternalControls(2, "Turntable1");
        // TT2
        fiab.turntable.StartupUtil.startupWithHiddenInternalControls(3, "Turntable2");
    }
}
