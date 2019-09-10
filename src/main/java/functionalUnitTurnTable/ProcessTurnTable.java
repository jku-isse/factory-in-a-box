package functionalUnitTurnTable;

import functionalUnitBase.ProcessEngineBase;
import open62Wrap.SWIGTYPE_p_UA_Server;
import open62Wrap.ServerAPIBase;
import open62Wrap.UA_NodeId;
import open62Wrap.open62541;
import uaMethods.processMethods.LoadProcessMethod;
import uaMethods.processMethods.ResetProcessMethod;
import uaMethods.processMethods.StopProcessMethod;

/**
 * TurnTable implementation of the Process Engine
 */
public class ProcessTurnTable extends ProcessEngineBase {
    private UA_NodeId statusNodeId;
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
        int b = open62541.UA_ACCESSLEVELMASK_WRITE | open62541.UA_ACCESSLEVELMASK_READ;
        statusNodeId = getServerAPIBase().addVariableNode(getServer(), processFolder, open62541.UA_NODEID_NUMERIC(1, 58),
                "ProcessEngineStatus", open62541.UA_TYPES_INT32, b);
        new LoadProcessMethod(this).addMethod(server, serverAPIBase, processFolder);
        new ResetProcessMethod(this).addMethod(server, serverAPIBase, processFolder);
        new StopProcessMethod(this).addMethod(server, serverAPIBase, processFolder);
    }
}
