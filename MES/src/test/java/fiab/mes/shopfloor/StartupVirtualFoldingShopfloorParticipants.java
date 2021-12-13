package fiab.mes.shopfloor;

import akka.actor.ActorSystem;
import fiab.core.capabilities.plotting.WellknownPlotterCapability;
import fiab.machine.foldingstation.opcua.StartupUtil;
import fiab.mes.productioncell.FoldingProductionCell;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StartupVirtualFoldingShopfloorParticipants {


    public static void main(String[] args) {
        startupSingleTurntableInputOutputDualFolding();
    }

    public static void startupSingleTurntableInputOutputDualFolding() {
        // TT1 West
        fiab.machine.iostation.opcua.StartupUtil.startupInputstation(0, "InputStation");

        // TT1 North
        fiab.machine.plotter.opcua.StartupUtil.startup(1, "Plotter", WellknownPlotterCapability.SupportedColors.RED);
        // TT1 South
        fiab.machine.plotter.opcua.StartupUtil.startup(5, "Plotter", WellknownPlotterCapability.SupportedColors.BLACK);

        // TT2 North
        fiab.machine.iostation.opcua.StartupUtil.startupOutputstation(2, "OutputStation");
        // TT2 South
        fiab.machine.plotter.opcua.StartupUtil.startup(6, "Plotter", WellknownPlotterCapability.SupportedColors.BLUE);
        // TT2 East
        StartupUtil.startup(9, "VirtualFolding");
        // TT2 East
        StartupUtil.startup(10, "VirtualFolding");
        // TT2 East
        StartupUtil.startup(11, "VirtualFolding");
        ScheduledExecutorService es = Executors.newScheduledThreadPool(4);
        startupHiddenFoldingParticipants(es);
        // TT3 West
        fiab.machine.iostation.opcua.StartupUtil.startupOutputstation(12, "TransitStation");
        // TT3 East
        fiab.machine.iostation.opcua.StartupUtil.startupOutputstation(14, "OutputStation");

        //Start Turntables later
        es.schedule(() -> {
            // TT2 West - TT1 itself
            fiab.turntable.StartupUtil.startupWithHiddenInternalControls(3, "VirtualTurntable1");
            // TT3
            fiab.turntable.StartupUtil.startupWithHiddenInternalControls(13, "VirtualTurntable3");
        }, 5, TimeUnit.SECONDS);
        //Start TT2 even later, since TT1 is required
        es.schedule(() -> {
            // TT1 East - TT2 itself
            fiab.turntable.StartupUtil.startupWithHiddenInternalControls(4, "VirtualTurntable2");
        }, 10, TimeUnit.SECONDS);
    }

    private static void startupHiddenFoldingParticipants(ScheduledExecutorService es){
        //TT2 East, Actual HS_East binding for TT2 (For now since InputStation does not accept to be loaded via hs)
        fiab.machine.iostation.opcua.StartupUtil.startupOutputstation(20, "OutputStation");
        //TT4 West, Actual HS_East binding for TT2
        fiab.machine.iostation.opcua.StartupUtil.startupInputstation(7, "InputStation");
        // Start Turntable later
        es.schedule(() -> {
            //TT4 itself
            fiab.turntable.StartupUtil.startupWithHiddenInternalControls(8, "VirtualTurntable4");
        }, 10, TimeUnit.SECONDS);
    }
}
