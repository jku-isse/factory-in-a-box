package inputStation.opcua;

import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.functionalunit.connector.FUConnector;
import fiab.functionalunit.connector.MachineEventBus;
import fiab.handshake.server.messages.ServerHandshakeStatusUpdateEvent;
import fiab.iostation.opcua.OpcUaOutputStationActor;
import fiab.opcua.client.FiabOpcUaClient;
import fiab.opcua.server.OPCUABase;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.junit.jupiter.api.*;
import testutils.FUTestInfrastructure;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("IntegrationTest")
public class OutputStationTest {

    private static FUTestInfrastructure infrastructure;

    //Playground
    public static void main(String[] args) {
        FUTestInfrastructure infrastructure = new FUTestInfrastructure(4840);
        FUConnector requestConnector = new FUConnector();
        infrastructure.subscribeToIntraMachineEventBus();
        OPCUABase opcuaBase = infrastructure.getServer();
        infrastructure.initializeActor(OpcUaOutputStationActor.props(opcuaBase, opcuaBase.getRootNode(), new MachineEventBus()),
                "OutputStation" + infrastructure.getAndIncrementRunCount());
    }

    @BeforeAll
    public static void init() {
        infrastructure = new FUTestInfrastructure(4848);
    }

    @BeforeEach
    public void setup() {
        infrastructure.subscribeToMachineEventBus();
        OPCUABase opcuaBase = infrastructure.getServer();
        infrastructure.initializeActor(OpcUaOutputStationActor.props(opcuaBase, opcuaBase.getRootNode(), infrastructure.getMachineEventBus()),
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
    public void testOutputStation() {
        assertDoesNotThrow(() -> {
            client().callStringMethodBlocking(OutputStationOpcUaNodes.resetNodeId);
            expectServerSideState(ServerSideStates.RESETTING);
            expectServerSideState(ServerSideStates.IDLE_EMPTY);

            client().callStringMethodBlocking(OutputStationOpcUaNodes.initNodeId);
            expectServerSideState(ServerSideStates.STARTING);
            expectServerSideState(ServerSideStates.PREPARING);
            expectServerSideState(ServerSideStates.READY_EMPTY);

            client().callStringMethodBlocking(OutputStationOpcUaNodes.startNodeId);
            expectServerSideState(ServerSideStates.EXECUTE);
            expectServerSideState(ServerSideStates.COMPLETING);
            expectServerSideState(ServerSideStates.COMPLETE);
            String state = client().readStringVariableNode(OutputStationOpcUaNodes.stateNodeId);
            assertEquals(state, ServerSideStates.COMPLETE.name());

            client().callStringMethodBlocking(OutputStationOpcUaNodes.resetNodeId);
            expectServerSideState(ServerSideStates.RESETTING);
            expectServerSideState(ServerSideStates.IDLE_EMPTY);
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

    static class OutputStationOpcUaNodes {
        static final NodeId resetNodeId = NodeId.parse("ns=2;s=TestDevice/RESET");
        static final NodeId stopNodeId = NodeId.parse("ns=2;s=TestDevice/STOP");
        static final NodeId stateNodeId = NodeId.parse("ns=2;s=TestDevice/STATE");
        static final NodeId initNodeId = NodeId.parse("ns=2;s=TestDevice/INIT_HANDOVER");
        static final NodeId startNodeId = NodeId.parse("ns=2;s=TestDevice/START_HANDOVER");
        static final NodeId completeNodeId = NodeId.parse("ns=2;s=TestDevice/COMPLETE");
    }
}
