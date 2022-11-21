package conveyor.opcua;

import fiab.conveyor.statemachine.ConveyorStates;
import fiab.core.capabilities.BasicMachineStates;
import fiab.opcua.client.FiabOpcUaClient;
import fiab.opcua.client.OPCUAClientFactory;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Tag("SystemTest")
public class TestRemoteTurntableConveyor {

    //This endpoint must match the robot endpoint
    private final String remoteEndpointUrl = "opc.tcp://192.168.133.54:4840";
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
    public void testRemoteLoadConveyor() {
        assertDoesNotThrow(() -> {
            prepareMachineForTest();
            client.callStringMethodBlocking(TurntableOpcUaNodes.resetNode);
            waitForMachineToReachState(BasicMachineStates.IDLE);
            client.callStringMethodBlocking(ConveyorFUOpcUaNodes.loadNodeId);
            waitForConveyorFUToReachState(ConveyorStates.IDLE_EMPTY);
        });
    }

    @Test
    public void testRemoteUnloadConveyor() {
        assertDoesNotThrow(() -> {
            prepareMachineForTest();
            client.callStringMethodBlocking(TurntableOpcUaNodes.resetNode);
            waitForMachineToReachState(BasicMachineStates.IDLE);
            client.callStringMethodBlocking(ConveyorFUOpcUaNodes.unloadNodeId);
            waitForConveyorFUToReachState(ConveyorStates.IDLE_EMPTY);
        });
    }

    @Test
    public void testRemoteConveyorStepwise() {
        //Load and Unload n times and see if issue occurs. Start with unloaded conveyor
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 20; i++) {
                prepareMachineForTest();
                client.callStringMethodBlocking(TurntableOpcUaNodes.resetNode);
                waitForMachineToReachState(BasicMachineStates.IDLE);
                if (i % 2 == 0) {
                    //FIXME
                    client.callStringMethodBlocking(ConveyorFUOpcUaNodes.loadNodeId);
                    waitForConveyorFUToReachState(ConveyorStates.IDLE_FULL);
                }else{
                    //FIXME
                    client.callStringMethodBlocking(ConveyorFUOpcUaNodes.unloadNodeId);
                    waitForConveyorFUToReachState(ConveyorStates.IDLE_EMPTY);
                }
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

    private void waitForConveyorFUToReachState(ConveyorStates state) throws UaException {
        String currentState = BasicMachineStates.UNKNOWN.toString();
        while (!currentState.equalsIgnoreCase(state.toString())) {
            currentState = client.readStringVariableNode(ConveyorFUOpcUaNodes.stateNodeId);
        }
    }

    static class TurntableOpcUaNodes {
        public static NodeId resetNode = NodeId.parse("ns=2;s=Turntable1/Reset");
        public static NodeId stopNode = NodeId.parse("ns=2;s=Turntable1/Stop");
        public static NodeId transportReq = NodeId.parse("ns=2;s=Turntable1/TransportRequest");
        public static NodeId statusNode = NodeId.parse("ns=2;s=Turntable1/STATE");
    }

    static class ConveyorFUOpcUaNodes { //FIXME
        static final NodeId resetNodeId = NodeId.parse("ns=2;s=Turntable1/ConveyorFU/RESET");
        static final NodeId stopNodeId = NodeId.parse("ns=2;s=Turntable1/ConveyorFU/STOP");
        static final NodeId stateNodeId = NodeId.parse("ns=2;s=Turntable1/ConveyorFU/STATE");
        static final NodeId loadNodeId = NodeId.parse("ns=2;s=Turntable1/ConveyorFU/LOAD");
        static final NodeId unloadNodeId = NodeId.parse("ns=2;s=Turntable1/ConveyorFU/UNLOAD");
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
