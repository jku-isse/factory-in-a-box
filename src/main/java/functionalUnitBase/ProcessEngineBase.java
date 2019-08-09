package functionalUnitBase;

import open62Wrap.SWIGTYPE_p_UA_Server;
import open62Wrap.ServerAPIBase;

public abstract class ProcessEngineBase {

    public abstract void loadProcess();

    public abstract void reset();   //TODO delete this if not necessary

    public abstract void stop();

    public abstract void addServerConfig(SWIGTYPE_p_UA_Server server, ServerAPIBase serverAPIBase);
}
