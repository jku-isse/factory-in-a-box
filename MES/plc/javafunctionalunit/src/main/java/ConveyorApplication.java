import functionalUnits.ConveyorTurnTable;
import functionalUnits.ProcessTurnTable;
import hardware.actuators.motorsEV3.MediumMotorEV3;
import hardware.sensors.sensorsEV3.ColorSensorEV3;
import hardware.sensors.sensorsEV3.TouchSensorEV3;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import robot.Robot;

public class ConveyorApplication {

    public static void main(String[] args) {
        Robot robot = new Robot(new ConveyorTurnTable(new MediumMotorEV3(MotorPort.A),
                new TouchSensorEV3(SensorPort.S2), new ColorSensorEV3(SensorPort.S3)),
                new ProcessTurnTable());

    //    robot.addHandshakeFU(CapabilityId.NORTH_SERVER, new HandshakeFU(robot.getServerCommunication(),
     //           robot.getServer(), robot.getRobotRoot(), CapabilityId.NORTH_SERVER));

       // robot.addHandshakeFU(CapabilityId.NORTH_CLIENT, new HandshakeFU(robot.getCommunication(),
       //         robot.getServer(), robot.getClient(), robot.getRobotRoot(), CapabilityId.NORTH_SERVER));

        robot.runServerAndClient();
    }
}
