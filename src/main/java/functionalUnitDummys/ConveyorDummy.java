package functionalUnitDummys;

import functionalUnitBase.ConveyorBase;
import functionalUnitDummys.conveyorMethods.*;
import open62Wrap.*;

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
        System.out.println("Resetting conveyor");
    }

    @Override
    public void stop() {
        System.out.println("Stopping conveyor");
    }

    @Override
    public void addServerConfig(SWIGTYPE_p_UA_Server server ,ServerAPIBase serverAPIBase) {
        LoadMethod.addMethod(server, serverAPIBase);
        UnloadMethod.addMethod(server, serverAPIBase);
        PauseMethod.addMethod(server, serverAPIBase);
        ResetConveyorMethod.addMethod(server, serverAPIBase);
        StopConveyorMethod.addMethod(server, serverAPIBase);
    }


}
