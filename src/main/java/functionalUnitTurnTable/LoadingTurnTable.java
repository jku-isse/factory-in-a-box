package functionalUnitTurnTable;

import functionalUnitBase.LoadingProtocolBase;
import open62Wrap.SWIGTYPE_p_UA_Server;
import open62Wrap.ServerAPIBase;
import open62Wrap.UA_NodeId;
import open62Wrap.open62541;
import turnTable.TurnTableOrientation;
import uaMethods.loadingMethods.*;

/**
 * TurnTable implementation of the Loading Protocol FU.
 */
public class LoadingTurnTable extends LoadingProtocolBase {

    private UA_NodeId statusNodeId;

    /**
     * {@inheritDoc}
     */
    @Override
    public void initiateLoading(TurnTableOrientation direction, int orderId) {
        System.out.println("Initiate loading was called");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initiateUnloading(TurnTableOrientation direction, int orderId) {
        System.out.println("Initiate unloading was called");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void complete() {
        System.out.println("Complete was called");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        System.out.println("Loading: Reset was called");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        System.out.println("Loading: Stop was called");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addServerConfig(SWIGTYPE_p_UA_Server server, ServerAPIBase serverAPIBase, UA_NodeId loadingFolder) {
        int b = open62541.UA_ACCESSLEVELMASK_WRITE | open62541.UA_ACCESSLEVELMASK_READ;
        statusNodeId = getServerAPIBase().addVariableNode(getServer(), loadingFolder, open62541.UA_NODEID_NUMERIC(1, 55),
                "LoadingProtocolStatus", open62541.UA_TYPES_INT32, b);
        new CompleteMethod(this).addMethod(server, serverAPIBase, loadingFolder);
        new InitiateLoadingMethod(this).addMethod(server, serverAPIBase, loadingFolder);
        new InitiateUnloadingMethod(this).addMethod(server, serverAPIBase, loadingFolder);
        new ResetLoadingProtocolMethod(this).addMethod(server, serverAPIBase, loadingFolder);
        new StopLoadingProtocolMethod(this).addMethod(server, serverAPIBase, loadingFolder);
    }
}
