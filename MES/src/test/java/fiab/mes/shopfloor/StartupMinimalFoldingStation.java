package fiab.mes.shopfloor;

import fiab.machine.foldingstation.opcua.StartupUtil;

import java.util.concurrent.TimeUnit;

public class StartupMinimalFoldingStation {

    public static void main(String[] args) {
        startupSingleTurntableInputOutputDualFolding();
    }

    public static void startupSingleTurntableInputOutputDualFolding() {
        // TT1 West
        fiab.machine.iostation.opcua.StartupUtil.startupInputstation(0, "InputStation");
        // TT1 North
        StartupUtil.startup(5, "VirtualFolding1");
        // TT1 South
        fiab.machine.iostation.opcua.StartupUtil.startupOutputstation(7, "BufferStation");
        // OutputStation East TT2
        fiab.machine.iostation.opcua.StartupUtil.startupOutputstation(1, "OutputStation");
        // TT1 itself 1
        fiab.turntable.StartupUtil.startupWithHiddenInternalControls(2, "FoldingTurntable1");
        try {
            TimeUnit.SECONDS.sleep(10);
            //Thread.sleep(5000);
            // TT2 itself - ensure this starts later than the others or has no prior wiring configured
            fiab.turntable.StartupUtil.startupWithHiddenInternalControls(3, "FoldingTurntable2");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }



    }
}
