package fiab.mes.shopfloor;

import akka.actor.ActorSystem;
import fiab.iostation.InputStationFactory;
import fiab.iostation.OutputStationFactory;
import fiab.machine.foldingstation.opcua.StartupUtil;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StartupMinimalFoldingStation {

    public static void main(String[] args) {
        startupSingleTurntableInputOutputDualFolding();
    }

    public static void startupSingleTurntableInputOutputDualFolding(){
        startupSingleTurntableInputOutputDualFolding(ActorSystem.create());
    }

    public static void startupSingleTurntableInputOutputDualFolding(ActorSystem system) {
        // TT1 West
        //fiab.machine.iostation.opcua.StartupUtil.startupInputstation(0, "InputStation");
        InputStationFactory.startStandaloneInputStation(system, 4840, "InputStation");
        // TT1 North
        StartupUtil.startup(5, "VirtualFolding1");
        // TT1 South
        OutputStationFactory.startStandaloneOutputStation(system, 4847, "TransitStation");
        // OutputStation East TT2
        OutputStationFactory.startStandaloneOutputStation(system, 4841, "OutputStation");
        // TT1 itself 1
        fiab.turntable.StartupUtil.startupWithHiddenInternalControls(2, "FoldingTurntable1");

        ScheduledExecutorService es = Executors.newScheduledThreadPool(1);
        es.schedule(new Runnable(){
            @Override
            public void run() {
                fiab.turntable.StartupUtil.startupWithHiddenInternalControls(3, "FoldingTurntable2");
            }
        }, 10, TimeUnit.SECONDS);
//        try {
//            //TimeUnit.SECONDS.sleep(10);
//            Thread.sleep(5000);
//            // TT2 itself - ensure this starts later than the others or has no prior wiring configured
//            fiab.turntable.StartupUtil.startupWithHiddenInternalControls(3, "FoldingTurntable2");
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }



    }
}
