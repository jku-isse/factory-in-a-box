package fiab.mes.shopfloor;

import akka.actor.ActorSystem;
import fiab.core.capabilities.plotting.WellknownPlotterCapability;
import fiab.iostation.InputStationFactory;
import fiab.iostation.OutputStationFactory;
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
        startupSingleTurntableInputOutputDualFolding(ActorSystem.create());
    }

    public static void startupSingleTurntableInputOutputDualFolding(ActorSystem system) {
        // TT1 West
        InputStationFactory.startStandaloneInputStation(system, 4840, "InputStation");

        // TT1 North
        fiab.machine.plotter.opcua.StartupUtil.startup(1, "Plotter", WellknownPlotterCapability.SupportedColors.RED);
        // TT1 South
        fiab.machine.plotter.opcua.StartupUtil.startup(5, "Plotter", WellknownPlotterCapability.SupportedColors.BLACK);

        // TT2 North
        OutputStationFactory.startStandaloneOutputStation(system, 4842, "OutputStation");
        // TT2 South
        fiab.machine.plotter.opcua.StartupUtil.startup(6, "Plotter", WellknownPlotterCapability.SupportedColors.BLUE);
        // TT2 East
        StartupUtil.startup(9, "VirtualFolding");
        // TT2 East
        StartupUtil.startup(10, "VirtualFolding");
        // TT2 East
        StartupUtil.startup(11, "VirtualFolding");
        ScheduledExecutorService es = Executors.newScheduledThreadPool(4);
        startupHiddenFoldingParticipants(system, es);
        // TT3 West
        //FIXME
        InputStationFactory.startStandaloneInputStation(system, 4852, "InputStation");
        //fiab.machine.iostation.opcua.StartupUtil.startupInputStationNoAutoReload(12, "TransitStation");
        // TT3 East
        OutputStationFactory.startStandaloneOutputStation(system, 4854, "OutputStation");

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

    private static void startupHiddenFoldingParticipants(ActorSystem system, ScheduledExecutorService es) {
        //TT2 East, Actual HS_East binding for TT2 (For now since InputStation does not accept to be loaded via hs)
        OutputStationFactory.startStandaloneOutputStation(system, 4860, "OutputStation");
        //fiab.machine.iostation.opcua.StartupUtil.startupOutputstation(20, "OutputStation");
        //TT4 West, Actual HS_East binding for TT2
        InputStationFactory.startStandaloneInputStation(system, 4847, "InputStation");
        //fiab.machine.iostation.opcua.StartupUtil.startupInputStationNoAutoReload(7, "InputStation");
        // Start Turntable later
        es.schedule(() -> {
            //TT4 itself
            fiab.turntable.StartupUtil.startupWithHiddenInternalControls(8, "VirtualTurntable4");
        }, 10, TimeUnit.SECONDS);
    }
}
