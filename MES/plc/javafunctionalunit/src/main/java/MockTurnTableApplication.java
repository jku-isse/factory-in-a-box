import capabilities.HandshakeFU;
import functionalUnits.ConveyorTurnTable;
import functionalUnits.ProcessTurnTable;
import functionalUnits.TurningTurnTable;
import hardware.ConveyorMockHardware;
import hardware.TurningMockHardware;
import helper.CapabilityId;
import robot.Robot;

public class MockTurnTableApplication {

    public static void main(String[] args) {

        ConveyorMockHardware conveyorMockHardware = new ConveyorMockHardware(200, 3000);
        TurningMockHardware turningMockHardware = new TurningMockHardware(200);
        //Run this if you want to run it on a Lego Robot. Make sure the ports match your configuration
        Robot robot = new Robot(new ConveyorTurnTable(conveyorMockHardware.getConveyorMockMotor(),
                conveyorMockHardware.getMockSensorLoading(), conveyorMockHardware.getMockSensorUnloading()),
                new TurningTurnTable(turningMockHardware.getTurningMockMotor(), turningMockHardware.getMockSensorHoming()),
                new ProcessTurnTable());

        robot.addHandshakeFU(CapabilityId.NORTH_SERVER, new HandshakeFU(robot.getServerCommunication(),
                robot.getServer(), robot.getHandshakeRoot(), CapabilityId.NORTH_SERVER));

        robot.addHandshakeFU(CapabilityId.NORTH_CLIENT, new HandshakeFU(robot.getCommunication(),
                robot.getServer(), robot.getClient(), robot.getHandshakeRoot(), CapabilityId.NORTH_SERVER));

        robot.runServerAndClient();
    }
}
