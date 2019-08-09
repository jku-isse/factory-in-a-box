package functionalUnitBase;

import open62Wrap.SWIGTYPE_p_UA_Server;
import open62Wrap.ServerAPIBase;
import turnTable.TurnTableOrientation;

public abstract class LoadingProtocolBase {

    public abstract void initiateLoading(TurnTableOrientation direction, int orderId);

    public abstract void initiateUnloading(TurnTableOrientation direction, int orderId);

    public abstract void complete();

    public abstract void reset();   //TODO delete if not necessary

    public abstract void stop();

    public abstract void addServerConfig(SWIGTYPE_p_UA_Server server, ServerAPIBase serverAPIBase);
}
