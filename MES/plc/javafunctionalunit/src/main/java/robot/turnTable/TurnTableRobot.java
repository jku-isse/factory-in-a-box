package robot.turnTable;

import functionalUnits.ConveyorBase;
import functionalUnits.LoadingProtocolBase;
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
    public TurnTableRobot(LoadingProtocolBase loadingProtocolBase, ConveyorBase conveyorBase) {
        super(loadingProtocolBase, conveyorBase);
    }

    /**
     * {@inheritDoc}
     */
    public TurnTableRobot(LoadingProtocolBase loadingProtocolBase, ConveyorBase conveyorBase, TurningBase turningBase) {
        super(loadingProtocolBase, conveyorBase, turningBase);
    }

    /**
     * {@inheritDoc}
     */
    public TurnTableRobot(LoadingProtocolBase loadingProtocolBase, ConveyorBase conveyorBase, ProcessEngineBase processEngineBase) {
        super(loadingProtocolBase, conveyorBase, processEngineBase);
    }

    /**
     * {@inheritDoc}
     */
    public TurnTableRobot(LoadingProtocolBase loadingProtocolBase, ConveyorBase conveyorBase, TurningBase turningBase, ProcessEngineBase processEngineBase) {
        super(loadingProtocolBase, conveyorBase, turningBase, processEngineBase);
    }
}
