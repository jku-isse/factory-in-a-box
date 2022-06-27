package inputStation.opcua;

import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.functionalunit.connector.FUConnector;
import fiab.functionalunit.connector.MachineEventBus;
import fiab.handshake.server.messages.ServerHandshakeStatusUpdateEvent;
import fiab.iostation.opcua.OpcUaInputStationActor;
import fiab.opcua.client.FiabOpcUaClient;
import fiab.opcua.server.OPCUABase;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.junit.jupiter.api.*;
import testutils.FUTestInfrastructure;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("IntegrationTest")
public class InputStationTest {

    private static FUTestInfrastructure infrastructure;

    //Playground
    public static void main(String[] args) {
        FUTestInfrastructure infrastructure = new FUTestInfrastructure(4840);
        FUConnector requestConnector = new FUConnector();
        infrastructure.subscribeToIntraMachineEventBus();
        OPCUABase opcuaBase = infrastructure.getServer();
        infrastructure.initializeActor(OpcUaInputStationActor.props(opcuaBase, opcuaBase.getRootNode(), new MachineEventBus()),
                "InputStation" + infrastructure.getAndIncrementRunCount());
    }

    @BeforeAll
    public static void init() {
        infrastructure = new FUTestInfrastructure(4848);
    }

    @BeforeEach
    public void setup() {
        infrastructure.subscribeToMachineEventBus();
        OPCUABase opcuaBase = infrastructure.getServer();
        infrastructure.initializeActor(OpcUaInputStationActor.props(opcuaBase, opcuaBase.getRootNode(), infrastructure.getMachineEventBus()),
                "InputStation" + infrastructure.getAndIncrementRunCount());
        infrastructure.connectClient();
    }

    @AfterEach
    public void teardown() {
        infrastructure.getIntraMachineEventBus().unsubscribe(infrastructure.getProbe().getRef());
    }

    @AfterAll
    public static void cleanup() {
        infrastructure.shutdownInfrastructure();
    }

    @Test
    public void testInputStation() {
        assertDoesNotThrow(() -> {
            client().callStringMethodBlocking(InputStationOpcUaNodes.resetNodeId);
            expectServerSideState(ServerSideStates.RESETTING);
            expectServerSideState(ServerSideStates.IDLE_LOADED);

            client().callStringMethodBlocking(InputStationOpcUaNodes.initNodeId);
            expectServerSideState(ServerSideStates.STARTING);
            expectServerSideState(ServerSideStates.PREPARING);
            expectServerSideState(ServerSideStates.READY_LOADED);

            client().callStringMethodBlocking(InputStationOpcUaNodes.startNodeId);
            expectServerSideState(ServerSideStates.EXECUTE);
            expectServerSideState(ServerSideStates.COMPLETING);
            expectServerSideState(ServerSideStates.COMPLETE);
            String state = client().readStringVariableNode(InputStationOpcUaNodes.stateNodeId);
            assertEquals(state, ServerSideStates.COMPLETE.name());

            client().callStringMethodBlocking(InputStationOpcUaNodes.resetNodeId);
            expectServerSideState(ServerSideStates.RESETTING);
            expectServerSideState(ServerSideStates.IDLE_LOADED);
        });
    }

    private void expectServerSideState(ServerSideStates serverSideState) {
        ServerHandshakeStatusUpdateEvent machineStatusUpdateEvent;
        machineStatusUpdateEvent = probe().expectMsgClass(Duration.ofSeconds(10), ServerHandshakeStatusUpdateEvent.class);
        assertEquals(serverSideState, machineStatusUpdateEvent.getStatus());
    }

    private FiabOpcUaClient client() {
        return infrastructure.getClient();
    }

    private TestKit probe() {
        return infrastructure.getProbe();
    }

    static class InputStationOpcUaNodes {
        static final NodeId resetNodeId = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU/RESET");
        static final NodeId stopNodeId = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU/STOP");
        static final NodeId stateNodeId = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU/STATE");
        static final NodeId initNodeId = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU/INIT_HANDOVER");
        static final NodeId startNodeId = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU/START_HANDOVER");
        static final NodeId completeNodeId = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU/COMPLETE");
    }
}
