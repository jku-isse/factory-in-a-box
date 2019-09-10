package functionalUnitBase;

import open62Wrap.SWIGTYPE_p_UA_Server;
import open62Wrap.ServerAPIBase;
import open62Wrap.UA_NodeId;

/**
 * Abstract base class for the process engine functional Unit
 * All process engine FUs should extend this class
 */
public abstract class ProcessEngineBase {

    private SWIGTYPE_p_UA_Server server;
    private ServerAPIBase serverAPIBase;

    public void setServer(SWIGTYPE_p_UA_Server server) {
        this.server = server;
    }

    public void setServerAPIBase(ServerAPIBase serverAPIBase) {
        this.serverAPIBase = serverAPIBase;
    }

    public SWIGTYPE_p_UA_Server getServer() {
        return server;
    }

    public ServerAPIBase getServerAPIBase() {
        return serverAPIBase;
    }

    public abstract void loadProcess();

    public abstract void reset();   //TODO delete this if not necessary

    public abstract void stop();

    public abstract void addServerConfig(SWIGTYPE_p_UA_Server server, ServerAPIBase serverAPIBase, UA_NodeId processFolder);
}
