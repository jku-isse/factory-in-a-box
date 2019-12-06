import capabilities.HandshakeFU;
import functionalUnits.ConveyorTurnTable;
import functionalUnits.ProcessTurnTable;
import functionalUnits.TurningTurnTable;
import hardware.ConveyorMockHardware;
import hardware.TurningMockHardware;
import helper.CapabilityId;
import robot.Robot;

/**
 * This class can be used for testing out the functionality of the TurnTable when no hardware is available.
 * The Hardware is defined in their own classes and represent actuators and sensors which simulate behaviour.
 */
public class MockTurnTableApplication {

    public static void main(String[] args) {

        ConveyorMockHardware conveyorMockHardware = new ConveyorMockHardware(200, 3000);
        TurningMockHardware turningMockHardware = new TurningMockHardware(200);
        Robot robot = new Robot(new ConveyorTurnTable(conveyorMockHardware.getConveyorMockMotor(),
                conveyorMockHardware.getMockSensorLoading(), conveyorMockHardware.getMockSensorUnloading()),
                new TurningTurnTable(turningMockHardware.getTurningMockMotor(), turningMockHardware.getMockSensorHoming()),
                new ProcessTurnTable());


      //  robot.addHandshakeFU(CapabilityId.NORTH_SERVER, new HandshakeFU(robot.getServerCommunication(),
       //         robot.getServer(), robot.getRobotRoot(), CapabilityId.NORTH_SERVER));

   //     robot.addHandshakeFU(CapabilityId.NORTH_CLIENT, new HandshakeFU(robot.getCommunication(),
   //             robot.getServer(), robot.getClient(), robot.getHandshakeRoot(), CapabilityId.NORTH_SERVER));

        robot.runServerAndClient();
    }
}
