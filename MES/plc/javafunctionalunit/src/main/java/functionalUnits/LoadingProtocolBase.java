package functionalUnits;

import robot.turnTable.TurnTableOrientation;

/**
 * Abstract base class for the loading functional Unit
 * All loading FUs should extend this class
 */
public abstract class LoadingProtocolBase extends FunctionalUnitBase {

    /**
     * Initiates the loading protocol.
     * @param direction loading source
     * @param orderId unique order id
     */
    public abstract void initiateLoading(TurnTableOrientation direction, int orderId);

    /**
     * Initiates the unloading protocol.
     * @param direction unloading destination
     * @param orderId unique order id
     */
    public abstract void initiateUnloading(TurnTableOrientation direction, int orderId);

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
