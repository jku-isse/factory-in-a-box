package functionalUnits;

/**
 * Abstract base class for the conveyor functional Unit
 * All conveyor FUs should extend this class
 */

public abstract class ConveyorBase extends FunctionalUnitBase {

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
     */
    public abstract void addServerConfig();
}
