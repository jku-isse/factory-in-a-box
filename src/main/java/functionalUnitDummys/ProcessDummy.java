package functionalUnitDummys;

import functionalUnitBase.ProcessEngineBase;
import functionalUnitDummys.processMethods.LoadProcessMethod;
import functionalUnitDummys.processMethods.ResetProcessMethod;
import functionalUnitDummys.processMethods.StopProcessMethod;
import open62Wrap.SWIGTYPE_p_UA_Server;
import open62Wrap.ServerAPIBase;

public class ProcessDummy extends ProcessEngineBase {
    @Override
    public void loadProcess() {
        System.out.println("Load Process was called");
    }

    @Override
    public void reset() {
        System.out.println("Reset was called");
    }

    @Override
    public void stop() {
        System.out.println("Stop was called");
    }

    @Override
    public void addServerConfig(SWIGTYPE_p_UA_Server server, ServerAPIBase serverAPIBase) {
        LoadProcessMethod.addMethod(server, serverAPIBase);
        ResetProcessMethod.addMethod(server, serverAPIBase);
        StopProcessMethod.addMethod(server, serverAPIBase);
    }
}
