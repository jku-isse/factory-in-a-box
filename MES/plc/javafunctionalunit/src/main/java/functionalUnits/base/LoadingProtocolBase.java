package functionalUnits.base;

/**
 * Abstract base class for the loading functional Unit
 * All loading FUs should extend this class
 */
public abstract class LoadingProtocolBase extends FunctionalUnitBase {

    /**
     * Initiate hand over
     */
    public abstract void request_init_handover();

    /**
     * Start hand over
     */
    public abstract void request_start_handover();

    /**
     * Signals task completion. Robot should now be ready to take another task
     */
    public abstract void complete();

    /**
     * Resets the loading protocol
     */
    public abstract void reset();

    /**
     * Stops the current loading protocol. Reset is required in order to take tasks.
     */
    public abstract void stop();


    /**
     * Adds methods and variables to the server.
     * All nodes should be placed in the conveyor folder to enforce a clear structure.
     */
    public abstract void addServerConfig();

}
