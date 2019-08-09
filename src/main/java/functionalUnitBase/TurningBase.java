package functionalUnitBase;

import open62Wrap.SWIGTYPE_p_UA_Server;
import open62Wrap.ServerAPIBase;
import turnTable.TurnTableOrientation;

public abstract class TurningBase {

    public abstract void turnTo(TurnTableOrientation orientation);

    public abstract void reset();

    public abstract void stop();

    public abstract void addServerConfig(SWIGTYPE_p_UA_Server server, ServerAPIBase serverAPIBase);
}
