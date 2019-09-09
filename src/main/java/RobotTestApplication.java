import functionalUnitDummys.ConveyorDummy;
import functionalUnitDummys.LoadingDummy;
import functionalUnitDummys.ProcessDummy;
import functionalUnitDummys.TurningDummy;
import functionalUnitTurnTable.ConveyorTurnTable;
import functionalUnitTurnTable.LoadingTurnTable;
import functionalUnitTurnTable.ProcessTurnTable;
import functionalUnitTurnTable.TurningTurnTable;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import robotBase.RobotBase;
import turnTable.TurnTableRobot;

public class RobotTestApplication {

    public static void main(String[] args) {
        //new RobotBase(new LoadingDummy(), new ConveyorDummy(), new TurningDummy(), new ProcessDummy()).runServer();
        new TurnTableRobot(new LoadingTurnTable(), new ConveyorTurnTable(MotorPort.A, SensorPort.S2, SensorPort.S3),
                new TurningTurnTable(MotorPort.D, SensorPort.S4, 90), new ProcessTurnTable()).runServer();
    }
}
