import capabilities.HandshakeFU;
import functionalUnits.ConveyorTurnTable;
import functionalUnits.ProcessTurnTable;
import functionalUnits.TurningTurnTable;
import hardware.actuators.motorsEV3.LargeMotorEV3;
import hardware.actuators.motorsEV3.MediumMotorEV3;
import hardware.sensors.sensorsEV3.ColorSensorEV3;
import hardware.sensors.sensorsEV3.TouchSensorEV3;
import helper.CapabilityId;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import robot.Robot;

/**
 * This is the main entrance point for the TurnTableRobot
 * In order to run it without the ev3 dependencies, run one of the examples without lego ports.
 * You might also need to get rid of all ev3/lejos dependencies when not running on lego hardware
 */
public class TurnTableApplication {


    public static void main(String[] args) {

        //Run this if you want to run it on a Lego Robot. Make sure the ports match your configuration
        Robot robot = new Robot(new ConveyorTurnTable(new MediumMotorEV3(MotorPort.A),
                new TouchSensorEV3(SensorPort.S2), new ColorSensorEV3(SensorPort.S3)),
                new TurningTurnTable(new LargeMotorEV3(MotorPort.D), new TouchSensorEV3(SensorPort.S4)),
                new ProcessTurnTable());

        robot.addHandshakeFU(CapabilityId.NORTH_SERVER, new HandshakeFU(robot.getServerCommunication(),
                robot.getServer(), robot.getRobotRoot(), CapabilityId.NORTH_SERVER));

        robot.addHandshakeFU(CapabilityId.NORTH_CLIENT, new HandshakeFU(robot.getCommunication(),
                robot.getServer(), robot.getClient(), robot.getRobotRoot(), CapabilityId.NORTH_SERVER));

        robot.runServerAndClient();
    }
}
