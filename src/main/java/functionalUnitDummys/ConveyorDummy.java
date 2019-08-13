package functionalUnitDummys;

import functionalUnitBase.ConveyorBase;
import uaMethods.conveyorMethods.*;
import open62Wrap.*;

import java.util.HashMap;
import java.util.function.Function;

public class ConveyorDummy extends ConveyorBase {

    @Override
    public void load() {
        System.out.println("Load was called");
    }

    @Override
    public void unload() {
        System.out.println("Unload was called");
    }

    @Override
    public void pause() {
        System.out.println("Pause was called");
    }

    @Override
    public void reset() {
        System.out.println("Reset was called in Conveyor");
    }

    @Override
    public void stop() {
        System.out.println("Stop was called in Conveyor");
    }

    @Override
    public void addServerConfig(SWIGTYPE_p_UA_Server server, ServerAPIBase serverAPIBase, UA_NodeId conveyorFolder) {
        new LoadMethod(this).addMethod(server, serverAPIBase, conveyorFolder);
        new UnloadMethod(this).addMethod(server, serverAPIBase, conveyorFolder);
        new PauseMethod(this).addMethod(server, serverAPIBase, conveyorFolder);
        new ResetConveyorMethod(this).addMethod(server, serverAPIBase, conveyorFolder);
        new StopConveyorMethod(this).addMethod(server, serverAPIBase, conveyorFolder);
    }


}
