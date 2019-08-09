package functionalUnitBase;

import open62Wrap.SWIGTYPE_p_UA_Server;
import open62Wrap.ServerAPIBase;

public abstract class ConveyorBase {

    public abstract void load();

    public abstract void unload();

    public abstract void pause();

    public abstract void reset();

    public abstract void stop();

    public abstract void addServerConfig(SWIGTYPE_p_UA_Server server, ServerAPIBase serverAPIBase);
}
