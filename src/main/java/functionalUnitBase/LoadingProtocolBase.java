package functionalUnitBase;

import open62Wrap.SWIGTYPE_p_UA_Server;
import open62Wrap.ServerAPIBase;
import open62Wrap.UA_NodeId;
import turnTable.TurnTableOrientation;

/**
 * Abstract base class for the loading functional Unit
 * All loading FUs should extend this class
 */
public abstract class LoadingProtocolBase {

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

    public abstract void initiateLoading(TurnTableOrientation direction, int orderId);

    public abstract void initiateUnloading(TurnTableOrientation direction, int orderId);

    public abstract void complete();

    public abstract void reset();   //TODO delete if not necessary

    public abstract void stop();

    public abstract void addServerConfig(SWIGTYPE_p_UA_Server server, ServerAPIBase serverAPIBase, UA_NodeId loadingFolder);
}
