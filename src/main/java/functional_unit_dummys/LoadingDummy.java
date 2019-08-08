package functional_unit_dummys;

import functional_unit_base.LoadingProtocolBase;
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
}
