package functionalUnits;

import com.github.oxo42.stateless4j.StateMachine;
import lombok.Getter;
import robot.turnTable.TurnTableOrientation;
import stateMachines.turning.TurningStates;
import stateMachines.turning.TurningTriggers;

/**
 * Abstract base class for the turning functional Unit
 * All turning FUs should extend this class
 */
public abstract class TurningBase extends FunctionalUnitBase {

    @Getter protected StateMachine<TurningStates, TurningTriggers> turningStateMachine;
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
