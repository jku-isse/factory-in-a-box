import capabilities.HandshakeCapability;
import capabilities.HandshakeFU;
import functionalUnits.ConveyorTurnTable;
import functionalUnits.ProcessTurnTable;
import functionalUnits.TurningTurnTable;
import hardware.ConveyorMockHardware;
import hardware.TurningMockHardware;
import helper.CapabilityId;
import helper.CapabilityRole;
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

        HandshakeFU hsFU = new HandshakeFU(robot.getCommunication(), robot.getServer(), robot.getClient(), robot.getRobotRoot());
        HandshakeCapability handshakeCapability = hsFU.addHanshakeEndpoint(CapabilityId.NORTH_CLIENT, CapabilityRole.Provided);
        HandshakeCapability handshakeCapabilityClient = hsFU.addHanshakeEndpoint(CapabilityId.NORTH_SERVER, CapabilityRole.Required);
        hsFU.addWiringCapability(CapabilityId.NORTH_SERVER, handshakeCapability.getEndpoint_NodeId(), handshakeCapability.getCapabilities_NodeId());
        hsFU.addWiringCapability(CapabilityId.NORTH_CLIENT, handshakeCapabilityClient.getEndpoint_NodeId(), handshakeCapabilityClient.getCapabilities_NodeId());

        robot.runServerAndClient();
    }
}
