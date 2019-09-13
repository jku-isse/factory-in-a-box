import functionalUnits.functionalUnitTurnTable.ConveyorTurnTable;
import functionalUnits.functionalUnitTurnTable.LoadingTurnTable;
import functionalUnits.functionalUnitTurnTable.ProcessTurnTable;
import functionalUnits.functionalUnitTurnTable.TurningTurnTable;
import hardware.actuators.MockMotor;

import hardware.sensors.MockSensor;

import robot.turnTable.TurnTableRobot;

/**
 *  This is the main entrance point for the TurnTableRobot
 *  In order to run it without the ev3 dependencies, run one of the examples without lego ports.
 *  You might also need to get rid of all ev3/lejos dependencies when not running on lego hardware
 */
public class RobotTestApplication {


    public static void main(String[] args) {
        //Run this if you just want the robot base without simulated motors
        //new RobotBase(new LoadingDummy(), new ConveyorDummy(), new TurningDummy(), new ProcessDummy()).runServer();

        //Run this if you want to run it on a Lego Robot. Make sure the ports match your configuration
        /*new TurnTableRobot(new LoadingTurnTable(),
                new ConveyorTurnTable(new MediumMotorEV3(MotorPort.A),
                        new TouchSensorEV3(SensorPort.S2) ,new ColorSensorEV3(SensorPort.S3)),
                new TurningTurnTable(new LargeMotorEV3(MotorPort.D) ,new TouchSensorEV3(SensorPort.S4)),
                new ProcessTurnTable()).runServer();
*/
        //Run this to simulate the hardware
        new TurnTableRobot(new LoadingTurnTable(),
                new ConveyorTurnTable(new MockMotor(), new MockSensor(), new MockSensor()),
                new TurningTurnTable(new MockMotor(), new MockSensor()),
                new ProcessTurnTable()).runServer();
    }
}
