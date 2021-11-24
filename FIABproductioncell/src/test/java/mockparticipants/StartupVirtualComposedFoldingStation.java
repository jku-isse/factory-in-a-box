package mockparticipants;

import fiab.machine.foldingstation.opcua.StartupUtil;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StartupVirtualComposedFoldingStation {

    public static void main(String[] args) {
        startupSingleTurntableInputOutputDualFolding();
    }

    public static void startupSingleTurntableInputOutputDualFolding() {
        // TT1 West
        fiab.machine.iostation.opcua.StartupUtil.startupInputstation(0, "InputStation");
        // TT1 North
        StartupUtil.startup(5, "VirtualFolding1");
        // TT1 South
        StartupUtil.startup(7, "VirtualFolding2");
        // TT2 EAST
        fiab.machine.iostation.opcua.StartupUtil.startupOutputstation(10, "BufferStation");
        // TT1 itself - ensure this starts later than the others or has no prior wiring configured
        ScheduledExecutorService es = Executors.newScheduledThreadPool(1);
        es.schedule(new Runnable(){
            @Override
            public void run() {
                fiab.turntable.StartupUtil.startupWithHiddenInternalControls(2, "Turntable1");
            }
        }, 3, TimeUnit.SECONDS);
    }
}
