package functionalUnitBase;

import com.github.oxo42.stateless4j.StateMachine;
import open62Wrap.SWIGTYPE_p_UA_Server;
import open62Wrap.ServerAPIBase;
import open62Wrap.UA_NodeId;

import java.util.HashMap;
import java.util.function.Function;

public abstract class ConveyorBase {

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

    public abstract void load();

    public abstract void unload();

    public abstract void pause();

    public abstract void reset();

    public abstract void stop();

    public abstract void addServerConfig(SWIGTYPE_p_UA_Server server, ServerAPIBase serverAPIBase, UA_NodeId conveyorFolder);
}
