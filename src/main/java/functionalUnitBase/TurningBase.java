package functionalUnitBase;

import open62Wrap.SWIGTYPE_p_UA_Server;
import open62Wrap.ServerAPIBase;
import open62Wrap.UA_NodeId;
import turnTable.TurnTableOrientation;

/**
 * Abstract base class for the turning functional Unit
 * All turning FUs should extend this class
 */
public abstract class TurningBase {

    private SWIGTYPE_p_UA_Server server;
    private ServerAPIBase serverAPIBase;

    /**
     * Sets the server to add methods and variables
     * @param server server to use
     */
    public void setServer(SWIGTYPE_p_UA_Server server) {
        this.server = server;
    }

    /**
     * Sets the serverAPIBase to add methods and Variables
     * @param serverAPIBase serverAPIBase to use
     */
    public void setServerAPIBase(ServerAPIBase serverAPIBase) {
        this.serverAPIBase = serverAPIBase;
    }

    /**
     * Returns the server. If server is not set it will return null
     * @return server
     */
    public SWIGTYPE_p_UA_Server getServer() {
        return server;
    }

    /**
     * Returns the serverAPIBase. If no APIBase was assigned, it will return null
     * @return serverAPIBase
     */
    public ServerAPIBase getServerAPIBase() {
        return serverAPIBase;
    }

    /**
     * Where the robot should turn to.
     * @param orientation target destination
     */
    public abstract void turnTo(TurnTableOrientation orientation);

    /**
     * Resets the TurnTable.
     */
    public abstract void reset();

    /**
     * Stops the TurnTable. Reset is required to take another task.
     */
    public abstract void stop();

    /**
     * Adds methods and variables to the server.
     * All nodes should be placed in the conveyor folder to enforce a clear structure.
     * @param server server to add config to
     * @param serverAPIBase serverAPIBase of the server
     * @param turningFolder folder where nodes will be placed
     */
    public abstract void addServerConfig(SWIGTYPE_p_UA_Server server, ServerAPIBase serverAPIBase, UA_NodeId turningFolder);
}
