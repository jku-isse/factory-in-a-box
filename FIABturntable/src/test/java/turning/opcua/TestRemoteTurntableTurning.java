package turning.opcua;

import fiab.core.capabilities.BasicMachineStates;
import fiab.opcua.client.FiabOpcUaClient;
import fiab.opcua.client.OPCUAClientFactory;
import fiab.turntable.turning.statemachine.TurningStates;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("SystemTest")
public class TestRemoteTurntableTurning {

    //This endpoint must match the robot endpoint
    private final String remoteEndpointUrl = "opc.tcp://192.168.133.139:4840";
    private FiabOpcUaClient client;

    @BeforeEach
    public void setup() {
        try {
            client = OPCUAClientFactory.createFIABClientAndConnect(remoteEndpointUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @AfterEach
    public void teardown() {
        client.disconnectClient();
    }

    @Test
    public void testRemoteReset() {
        assertDoesNotThrow(() -> {
            prepareMachineForTest();
            client.callStringMethodBlocking(TurntableOpcUaNodes.resetNode);
            waitForMachineToReachState(BasicMachineStates.IDLE);
        });
    }

    @Test
    public void testRemoteTurningNorth() {
        assertDoesNotThrow(() -> {
            prepareMachineForTest();
            client.callStringMethodBlocking(TurntableOpcUaNodes.resetNode);
            waitForMachineToReachState(BasicMachineStates.IDLE);
            client.callStringMethodBlocking(TurningFUOpcUaNodes.turnToNodeId, new Variant("NORTH"));
            waitForTurningFUToReachState(TurningStates.COMPLETE);
        });
    }

    @Test
    public void testRemoteTurningEast() {
        assertDoesNotThrow(() -> {
            prepareMachineForTest();
            client.callStringMethodBlocking(TurntableOpcUaNodes.resetNode);
            waitForMachineToReachState(BasicMachineStates.IDLE);
            client.callStringMethodBlocking(TurningFUOpcUaNodes.turnToNodeId, new Variant("EAST"));
            waitForTurningFUToReachState(TurningStates.COMPLETE);
        });
    }

    @Test
    public void testRemoteTurningSouth() {
        assertDoesNotThrow(() -> {
            prepareMachineForTest();
            client.callStringMethodBlocking(TurntableOpcUaNodes.resetNode);
            waitForMachineToReachState(BasicMachineStates.IDLE);
            client.callStringMethodBlocking(TurningFUOpcUaNodes.turnToNodeId, new Variant("SOUTH"));
            waitForTurningFUToReachState(TurningStates.COMPLETE);
        });
    }

    @Test
    public void testRemoteTurningWest() {
        assertDoesNotThrow(() -> {
            prepareMachineForTest();
            client.callStringMethodBlocking(TurntableOpcUaNodes.resetNode);
            waitForMachineToReachState(BasicMachineStates.IDLE);
            client.callStringMethodBlocking(TurningFUOpcUaNodes.turnToNodeId, new Variant("WEST"));
            waitForTurningFUToReachState(TurningStates.COMPLETE);
        });
    }

    @Test
    public void testRemoteTurningStepwise() {
        //Do a number of random rotations and see if any issue occurs
        Random random = new Random();
        List<String> directions = List.of("NORTH", "EAST", "SOUTH", "WEST");
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 20; i++) {
                prepareMachineForTest();
                client.callStringMethodBlocking(TurntableOpcUaNodes.resetNode);
                waitForMachineToReachState(BasicMachineStates.IDLE);
                client.callStringMethodBlocking(TurningFUOpcUaNodes.turnToNodeId,
                        new Variant(directions.get(random.nextInt(4))));
                waitForTurningFUToReachState(TurningStates.COMPLETE);
            }
        });
    }

    //Before each run, we need to reset the machine if it is not in the idle state
    private void prepareMachineForTest() {
        try {
            String state = client.readStringVariableNode(TurntableOpcUaNodes.statusNode);
            if (state.equalsIgnoreCase(BasicMachineStates.STOPPED.toString()) ||
                    state.equalsIgnoreCase(BasicMachineStates.STOPPING.toString())) {
                //If machine is stopped or stopping we just wait for it to be in stopped
                waitForMachineToReachState(BasicMachineStates.STOPPED);
            } else {
                client.callStringMethodBlocking(TurntableOpcUaNodes.stopNode);
                waitForMachineToReachState(BasicMachineStates.STOPPED);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Could not connect to machine. Make sure the machine is connected and opc ua nodes match");
        }
    }

    private void waitForMachineToReachState(BasicMachineStates state) throws UaException {
        String currentState = BasicMachineStates.UNKNOWN.toString();
        while (!currentState.equalsIgnoreCase(state.toString())) {
            currentState = client.readStringVariableNode(TurntableOpcUaNodes.statusNode);
        }
    }

    private void waitForTurningFUToReachState(TurningStates state) throws UaException {
        String currentState = BasicMachineStates.UNKNOWN.toString();
        while (!currentState.equalsIgnoreCase(state.toString())) {
            currentState = client.readStringVariableNode(TurningFUOpcUaNodes.stateNodeId);
        }
    }

    static class TurntableOpcUaNodes {
        public static NodeId resetNode = NodeId.parse("ns=2;s=Turntable1/Reset");
        public static NodeId stopNode = NodeId.parse("ns=2;s=Turntable1/Stop");
        public static NodeId transportReq = NodeId.parse("ns=2;s=Turntable1/TransportRequest");
        public static NodeId statusNode = NodeId.parse("ns=2;s=Turntable1/STATE");
    }

    static class TurningFUOpcUaNodes {
        static final NodeId resetNodeId = NodeId.parse("ns=2;s=Turntable1/TurningFU/RESET");
        static final NodeId stopNodeId = NodeId.parse("ns=2;s=Turntable1/TurningFU/STOP");
        static final NodeId stateNodeId = NodeId.parse("ns=2;s=Turntable1/TurningFU/STATE");
        static final NodeId turnToNodeId = NodeId.parse("ns=2;s=Turntable1/TurningFU/TURN_TO");
    }

    static class TurntableNorthHandshakeOpcUaNodes {
        public static NodeId resetNode = NodeId.parse("ns=2;s=Turntable1/HANDSHAKE_FU_NORTH_CLIENT/RESET");
        public static NodeId stopNode = NodeId.parse("ns=2;s=Turntable1/HANDSHAKE_FU_NORTH_CLIENT/STOP");
        public static NodeId initHandover = NodeId.parse("ns=2;s=Turntable1/HANDSHAKE_FU_NORTH_CLIENT/INIT_HANDOVER");
        public static NodeId startHandover = NodeId.parse("ns=2;s=Turntable1/HANDSHAKE_FU_NORTH_CLIENT/START_HANDOVER");
        public static NodeId statusNode = NodeId.parse("ns=2;s=Turntable1/HANDSHAKE_FU_NORTH_CLIENT/STATE");
    }

    static class TurntableEastHandshakeOpcUaNodes {
        public static NodeId resetNode = NodeId.parse("ns=2;s=Turntable1/HANDSHAKE_FU_SOUTH_CLIENT/RESET");
        public static NodeId stopNode = NodeId.parse("ns=2;s=Turntable1/HANDSHAKE_FU_SOUTH_CLIENT/STOP");
        public static NodeId initHandover = NodeId.parse("ns=2;s=Turntable1/HANDSHAKE_FU_SOUTH_CLIENT/INIT_HANDOVER");
        public static NodeId startHandover = NodeId.parse("ns=2;s=Turntable1/HANDSHAKE_FU_SOUTH_SERVER/START_HANDOVER");
        public static NodeId statusNode = NodeId.parse("ns=2;s=Turntable1/HANDSHAKE_FU_SOUTH_CLIENT/STATE");
    }

    static class TurntableSouthHandshakeOpcUaNodes {
        public static NodeId resetNode = NodeId.parse("ns=2;s=Turntable1/HANDSHAKE_FU_SOUTH_CLIENT/RESET");
        public static NodeId stopNode = NodeId.parse("ns=2;s=Turntable1/HANDSHAKE_FU_SOUTH_CLIENT/STOP");
        public static NodeId initHandover = NodeId.parse("ns=2;s=Turntable1/HANDSHAKE_FU_SOUTH_CLIENT/INIT_HANDOVER");
        public static NodeId startHandover = NodeId.parse("ns=2;s=Turntable1/HANDSHAKE_FU_SOUTH_CLIENT/START_HANDOVER");
        public static NodeId statusNode = NodeId.parse("ns=2;s=Turntable1/HANDSHAKE_FU_SOUTH_CLIENT/STATE");
    }

    static class TurntableWestHandshakeOpcUaNodes {
        public static NodeId resetNode = NodeId.parse("ns=2;s=Turntable1/HANDSHAKE_FU_WEST_CLIENT/RESET");
        public static NodeId stopNode = NodeId.parse("ns=2;s=Turntable1/HANDSHAKE_FU_SOUTH_CLIENT/STOP");
        public static NodeId initHandover = NodeId.parse("ns=2;s=Turntable1/HANDSHAKE_FU_SOUTH_CLIENT/INIT_HANDOVER");
        public static NodeId startHandover = NodeId.parse("ns=2;s=Turntable1/HANDSHAKE_FU_SOUTH_CLIENT/START_HANDOVER");
        public static NodeId statusNode = NodeId.parse("ns=2;s=Turntable1/HANDSHAKE_FU_SOUTH_CLIENT/STATE");
    }

}
