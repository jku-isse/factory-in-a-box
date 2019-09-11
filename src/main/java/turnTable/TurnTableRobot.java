package turnTable;

import functionalUnitBase.ConveyorBase;
import functionalUnitBase.LoadingProtocolBase;
import functionalUnitBase.ProcessEngineBase;
import functionalUnitBase.TurningBase;
import robotBase.RobotBase;

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
