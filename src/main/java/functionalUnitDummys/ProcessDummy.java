package functionalUnitDummys;

import functionalUnitBase.ProcessEngineBase;
import open62Wrap.SWIGTYPE_p_UA_Server;
import open62Wrap.ServerAPIBase;
import open62Wrap.UA_NodeId;
import uaMethods.processMethods.LoadProcessMethod;
import uaMethods.processMethods.ResetProcessMethod;
import uaMethods.processMethods.StopProcessMethod;

public class ProcessDummy extends ProcessEngineBase {
    @Override
    public void loadProcess() {
        System.out.println("Load Process was called");
    }

    @Override
    public void reset() {
        System.out.println("Reset was called in Process");
    }

    @Override
    public void stop() {
        System.out.println("Stop was called in Process");
    }

    @Override
    public void addServerConfig(SWIGTYPE_p_UA_Server server, ServerAPIBase serverAPIBase, UA_NodeId processFolder) {
        new LoadProcessMethod(this).addMethod(server, serverAPIBase, processFolder);
        new ResetProcessMethod(this).addMethod(server, serverAPIBase, processFolder);
        new StopProcessMethod(this).addMethod(server, serverAPIBase, processFolder);
    }
}
