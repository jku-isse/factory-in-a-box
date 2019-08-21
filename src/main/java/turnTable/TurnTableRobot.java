package turnTable;

import functionalUnitBase.ConveyorBase;
import functionalUnitBase.LoadingProtocolBase;
import functionalUnitBase.ProcessEngineBase;
import functionalUnitBase.TurningBase;
import robotBase.RobotBase;

public class TurnTableRobot extends RobotBase {
    //TODO implement addConfiguration methods in turnTable FUs
    public TurnTableRobot(LoadingProtocolBase loadingProtocolBase, ConveyorBase conveyorBase) {
        super(loadingProtocolBase, conveyorBase);
    }

    public TurnTableRobot(LoadingProtocolBase loadingProtocolBase, ConveyorBase conveyorBase, TurningBase turningBase) {
        super(loadingProtocolBase, conveyorBase, turningBase);
    }

    public TurnTableRobot(LoadingProtocolBase loadingProtocolBase, ConveyorBase conveyorBase, ProcessEngineBase processEngineBase) {
        super(loadingProtocolBase, conveyorBase, processEngineBase);
    }

    public TurnTableRobot(LoadingProtocolBase loadingProtocolBase, ConveyorBase conveyorBase, TurningBase turningBase, ProcessEngineBase processEngineBase) {
        super(loadingProtocolBase, conveyorBase, turningBase, processEngineBase);
    }
}
