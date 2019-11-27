package robot.turnTable;

import functionalUnits.ConveyorBase;
import functionalUnits.ProcessEngineBase;
import functionalUnits.TurningBase;
import robot.RobotBase;

/**
 * Extends the RobotBase to represent a TurnTableRobot. May have some other functionality added later
 */
public class TurnTableRobot extends RobotBase {

    /**
     * {@inheritDoc}
     */
    public TurnTableRobot(ConveyorBase conveyorBase) {
        super(conveyorBase);
    }

    /**
     * {@inheritDoc}
     */
    public TurnTableRobot(ConveyorBase conveyorBase, TurningBase turningBase) {
        super(conveyorBase, turningBase);
    }

    /**
     * {@inheritDoc}
     */
    public TurnTableRobot(ConveyorBase conveyorBase, ProcessEngineBase processEngineBase) {
        super(conveyorBase, processEngineBase);
    }

    /**
     * {@inheritDoc}
     */
    public TurnTableRobot(ConveyorBase conveyorBase, TurningBase turningBase, ProcessEngineBase processEngineBase) {
        super(conveyorBase, turningBase, processEngineBase);
    }
}
