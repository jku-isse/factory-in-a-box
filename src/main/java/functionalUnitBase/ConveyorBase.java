package functionalUnitBase;

import open62Wrap.SWIGTYPE_p_UA_Server;
import open62Wrap.ServerAPIBase;
import open62Wrap.UA_NodeId;

/**
 * Abstract base class for the conveyor functional Unit
 * All conveyor FUs should extend this class
 */

public abstract class ConveyorBase {

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
     * Loads the conveyor. Should load the conveyor until it is fully loaded or interrupted
     */
    public abstract void load();

    /**
     * Unloads the conveyor. Should unload until it is fully unloaded
     */
    public abstract void unload();

    /**
     * Pauses the loading or unloading. When necessary, the loading or unloading process can be continued.
     * The conveyor is put into a suspended state until another action is performed
     */
    public abstract void pause();

    /**
     * Resets the Conveyor belt and State Machine.
     * Every time the conveyor should perform an action reset should be called.
     */
    public abstract void reset();

    /**
     * Stops all current tasks and puts the conveyor in the stopped state. Call reset in order to use the conveyor again
     */
    public abstract void stop();

    /**
     * Adds methods and variables to the server.
     * All nodes should be placed in the conveyor folder to enforce a clear structure.
     * @param server server to add config to
     * @param serverAPIBase serverAPIBase of the server
     * @param conveyorFolder folder where nodes will be placed
     */
    public abstract void addServerConfig(SWIGTYPE_p_UA_Server server, ServerAPIBase serverAPIBase, UA_NodeId conveyorFolder);
}
