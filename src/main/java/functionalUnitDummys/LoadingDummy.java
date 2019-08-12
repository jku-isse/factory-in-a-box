package functionalUnitDummys;

import functionalUnitBase.LoadingProtocolBase;
import open62Wrap.SWIGTYPE_p_UA_Server;
import open62Wrap.ServerAPIBase;
import open62Wrap.UA_NodeId;
import uaMethods.loadingMethods.*;
import turnTable.TurnTableOrientation;

public class LoadingDummy extends LoadingProtocolBase {
    @Override
    public void initiateLoading(TurnTableOrientation direction, int orderId) {
        System.out.println("Initiated loading to " + direction + ". Order id: " + orderId);
    }

    @Override
    public void initiateUnloading(TurnTableOrientation direction, int orderId) {
        System.out.println("Initiated unloading to " + direction + ". Order id: " + orderId);
    }

    @Override
    public void complete() {
        System.out.println("Completed");
    }

    @Override
    public void reset() {
        System.out.println("Reset was called in Loading");
    }

    @Override
    public void stop() {
        System.out.println("Stop was called in Loading");
    }

    @Override
    public void addServerConfig(SWIGTYPE_p_UA_Server server, ServerAPIBase serverAPIBase, UA_NodeId loadingFolder) {
        new CompleteMethod(this).addMethod(server, serverAPIBase, loadingFolder);
        new InitiateLoadingMethod(this).addMethod(server, serverAPIBase, loadingFolder);
        new InitiateUnloadingMethod(this).addMethod(server, serverAPIBase, loadingFolder);
        new ResetLoadingProtocolMethod(this).addMethod(server, serverAPIBase, loadingFolder);
        new StopLoadingProtocolMethod(this).addMethod(server, serverAPIBase, loadingFolder);
    }

}
