package functionalUnits;

import robot.turnTable.TurnTableOrientation;

/**
 * Abstract base class for the turning functional Unit
 * All turning FUs should extend this class
 */
public abstract class TurningBase extends FunctionalUnitBase {

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
     */
    public abstract void addServerConfig();
}
