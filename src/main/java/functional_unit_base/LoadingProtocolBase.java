package functional_unit_base;

import turnTable.TurnTableOrientation;

public abstract class LoadingProtocolBase {

    public abstract void initiateLoading(TurnTableOrientation direction, int orderId);

    public abstract void initiateUnloading(TurnTableOrientation direction, int orderId);

    public abstract void complete();

    public abstract void reset();   //TODO delete if not necessary

    public abstract void stop();
}
