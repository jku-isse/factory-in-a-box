package functionalUnitDummys;

import functionalUnitBase.LoadingProtocolBase;
import functionalUnitDummys.loadingMethods.*;
import open62Wrap.SWIGTYPE_p_UA_Server;
import open62Wrap.ServerAPIBase;
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
        System.out.println("Resetting Loading");
    }

    @Override
    public void stop() {
        System.out.println("Stopping Loading");
    }

    @Override
    public void addServerConfig(SWIGTYPE_p_UA_Server server, ServerAPIBase serverAPIBase) {
        CompleteMethod.addMethod(server, serverAPIBase);
        InitiateLoadingMethod.addMethod(server, serverAPIBase);
        InitiateUnloadingMethod.addMethod(server, serverAPIBase);
        ResetLoadingProtocolMethod.addMethod(server, serverAPIBase);
        StopLoadingProtocolMethod.addMethod(server, serverAPIBase);
    }

}
