package functionalUnitTurnTable;

import functionalUnitBase.LoadingProtocolBase;
import open62Wrap.SWIGTYPE_p_UA_Server;
import open62Wrap.ServerAPIBase;
import open62Wrap.UA_NodeId;
import turnTable.TurnTableOrientation;

import java.util.HashMap;
import java.util.function.Function;

public class LoadingTurnTable extends LoadingProtocolBase {


    //TODO implement handshake
    @Override
    public void initiateLoading(TurnTableOrientation direction, int orderId) {
        System.out.println("Initiate loading was called");
    }

    @Override
    public void initiateUnloading(TurnTableOrientation direction, int orderId) {
        System.out.println("Initiate unloading was called");
    }

    @Override
    public void complete() {
        System.out.println("Complete was called");
    }

    @Override
    public void reset() {
        System.out.println("Loading: Reset was called");
    }

    @Override
    public void stop() {
        System.out.println("Loading: Stop was called");
    }

    @Override
    public void addServerConfig(SWIGTYPE_p_UA_Server server, ServerAPIBase serverAPIBase, UA_NodeId loadingFolder) {

    }
}
