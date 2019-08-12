package functionalUnitTurnTable;

import functionalUnitBase.ProcessEngineBase;
import open62Wrap.SWIGTYPE_p_UA_Server;
import open62Wrap.ServerAPIBase;
import open62Wrap.UA_NodeId;

public class ProcessTurnTable extends ProcessEngineBase {
    @Override
    public void loadProcess() {
        //TODO figure out what to do here
        System.out.println("Loaded Process");
    }

    @Override
    public void reset() {
        System.out.println("Reset Process was called");
    }

    @Override
    public void stop() {
        System.out.println("Stop Process was called");
    }

    @Override
    public void addServerConfig(SWIGTYPE_p_UA_Server server, ServerAPIBase serverAPIBase, UA_NodeId processFolder) {

    }
}
