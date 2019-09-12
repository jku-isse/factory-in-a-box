package functionalUnits;

/**
 * Abstract base class for the process engine functional Unit
 * All process engine FUs should extend this class
 */
public abstract class ProcessEngineBase extends FunctionalUnitBase {

    /**
     * Loads a process.
     */
    public abstract void loadProcess();

    /**
     * Resets the process engine
     */
    public abstract void reset();

    /**
     * Stops the process engine. Requires a reset in order to pass new process.
     */
    public abstract void stop();

    /**
     * Adds methods and variables to the server.
     * All nodes should be placed in the conveyor folder to enforce a clear structure.
    */
    public abstract void addServerConfig();
}
