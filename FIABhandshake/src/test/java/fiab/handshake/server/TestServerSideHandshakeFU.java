package fiab.handshake.server;

import akka.actor.ActorRef;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.core.capabilities.handshake.server.TransportAreaStatusOverrideRequest;
import fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers;
import fiab.functionalunit.connector.FUConnector;
import fiab.handshake.server.messages.ServerHandshakeStatusUpdateEvent;
import fiab.handshake.server.opcua.functionalunit.ServerHandshakeFU;
import fiab.opcua.client.FiabOpcUaClient;
import fiab.opcua.server.OPCUABase;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.junit.jupiter.api.*;
import testutils.FUTestInfrastructure;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("IntegrationTest")
public class TestServerSideHandshakeFU {

    private static FUTestInfrastructure infrastructure;

    //Playground
    public static void main(String[] args) {
        FUTestInfrastructure infrastructure = new FUTestInfrastructure(4840);
        FUConnector requestConnector = new FUConnector();
        infrastructure.subscribeToIntraMachineEventBus();
        OPCUABase opcuaBase = infrastructure.getServer();
        infrastructure.initializeActor(ServerHandshakeFU.props(opcuaBase, opcuaBase.getRootNode(),
                        TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_NORTH_SERVER,
                        requestConnector, infrastructure.getIntraMachineEventBus()),
                "ServerHandshakeFU" + infrastructure.getAndIncrementRunCount());
    }

    @BeforeAll
    public static void init(){
        infrastructure = new FUTestInfrastructure(4846);
    }

    @BeforeEach
    public void setup(){
        FUConnector requestConnector = new FUConnector();
        infrastructure.subscribeToIntraMachineEventBus();
        OPCUABase opcuaBase = infrastructure.getServer();
        infrastructure.initializeActor(ServerHandshakeFU.props(opcuaBase, opcuaBase.getRootNode(),
                TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_NORTH_SERVER,
                        requestConnector, infrastructure.getIntraMachineEventBus()),
                "ServerHandshakeFU" + infrastructure.getAndIncrementRunCount());
        expectServerSideState(ServerSideStates.STOPPED);
        infrastructure.connectClient();
    }

    @AfterEach
    public void teardown(){
        infrastructure.getIntraMachineEventBus().unsubscribe(infrastructure.getProbe().getRef());
    }

    @AfterAll
    public static void cleanup(){
        infrastructure.shutdownInfrastructure();
    }

    @Test
    public void testResetEmptySuccess() {
        assertDoesNotThrow(() -> {
            String state = client().readStringVariableNode(ServerHsOpcUaNodes.stateNodeId);
            assertEquals(ServerSideStates.STOPPED.toString(), state);

            resetHandshakeEmptyServerForTest();
        });
    }

    @Test
    public void testResetFullSuccess() {
        assertDoesNotThrow(() -> {
            String state = client().readStringVariableNode(ServerHsOpcUaNodes.stateNodeId);
            assertEquals(ServerSideStates.STOPPED.toString(), state);

            resetHandshakeLoadedServerForTest();
        });
    }

    @Test
    public void testFullHandshakeCycleEmptySuccess(){
        assertDoesNotThrow(() ->{
            resetHandshakeEmptyServerForTest();
            finishHandshakeCycleEmpty();
        });
    }

    @Test
    public void testFullHandshakeCycleLoadedSuccess(){
        assertDoesNotThrow(() ->{
            resetHandshakeLoadedServerForTest();
            finishHandshakeCycleLoaded();
        });
    }

    @Test
    public void testFullHandshakeCycleEmptyMultipleIterations(){
        assertDoesNotThrow(() ->{
            for (int i = 0; i < 3; i++) {
                resetHandshakeEmptyServerForTest();
                finishHandshakeCycleEmpty();
            }
        });
    }

    @Test
    public void testFullHandshakeCycleLoadedMultipleIterations(){
        assertDoesNotThrow(() ->{
            for (int i = 0; i < 3; i++) {
                resetHandshakeLoadedServerForTest();
                finishHandshakeCycleLoaded();
            }
        });
    }

    @Test
    public void testStopAfterResetEmpty(){
        assertDoesNotThrow(() ->{
            resetHandshakeEmptyServerForTest();
            client().callStringMethodBlocking(ServerHsOpcUaNodes.stopNodeId);
            expectServerSideState(ServerSideStates.STOPPING);
            expectServerSideState(ServerSideStates.STOPPED);
        });
    }

    @Test
    public void testStopAfterInitEmpty(){
        assertDoesNotThrow(() ->{
            resetHandshakeEmptyServerForTest();

            client().callStringMethodBlocking(ServerHsOpcUaNodes.initNodeId);
            expectServerSideState(ServerSideStates.STARTING);
            expectServerSideState(ServerSideStates.PREPARING);
            expectServerSideState(ServerSideStates.READY_EMPTY);

            client().callStringMethodBlocking(ServerHsOpcUaNodes.stopNodeId);
            expectServerSideState(ServerSideStates.STOPPING);
            expectServerSideState(ServerSideStates.STOPPED);
        });
    }

    @Test
    public void testStopAfterEmptyComplete(){
        assertDoesNotThrow(() ->{
            resetHandshakeEmptyServerForTest();
            finishHandshakeCycleEmpty();

            client().callStringMethodBlocking(ServerHsOpcUaNodes.stopNodeId);
            expectServerSideState(ServerSideStates.STOPPING);
            expectServerSideState(ServerSideStates.STOPPED);
        });
    }

    @Test
    public void testDoubleInitOnEmptyStopsHsAndFails(){
        assertDoesNotThrow(() ->{
            resetHandshakeEmptyServerForTest();
            String response = client().callStringMethodBlocking(ServerHsOpcUaNodes.initNodeId);
            assertEquals(HandshakeCapability.ServerMessageTypes.OkResponseInitHandover.toString(), response);
            response = client().callStringMethodBlocking(ServerHsOpcUaNodes.initNodeId);
            expectServerSideState(ServerSideStates.STARTING);
            expectServerSideState(ServerSideStates.PREPARING);
            expectServerSideState(ServerSideStates.READY_EMPTY);

            assertEquals(HandshakeCapability.ServerMessageTypes.NotOkResponseInitHandover.toString(), response);
            expectServerSideState(ServerSideStates.STOPPING);
            expectServerSideState(ServerSideStates.STOPPED);
        });
    }

    private void resetHandshakeEmptyServerForTest() throws Exception {
        //We need to reset first
        client().callStringMethodBlocking(ServerHsOpcUaNodes.resetNodeId);
        expectServerSideState(ServerSideStates.RESETTING);
        expectServerSideState(ServerSideStates.IDLE_EMPTY);
        String state = client().readStringVariableNode(ServerHsOpcUaNodes.stateNodeId);
        assertEquals(ServerSideStates.IDLE_EMPTY.toString(), state);
    }

    private void resetHandshakeLoadedServerForTest() throws Exception {
        //We need to reset first
        actorRef().tell(new TransportAreaStatusOverrideRequest(infrastructure.eventSourceId,
                HandshakeCapability.StateOverrideRequests.SetLoaded), ActorRef.noSender());
        client().callStringMethodBlocking(ServerHsOpcUaNodes.resetNodeId);
        expectServerSideState(ServerSideStates.RESETTING);
        expectServerSideState(ServerSideStates.IDLE_LOADED);
        String state = client().readStringVariableNode(ServerHsOpcUaNodes.stateNodeId);
        assertEquals(ServerSideStates.IDLE_LOADED.toString(), state);
    }

    private void finishHandshakeCycleEmpty() throws Exception{
        String response = client().callStringMethodBlocking(ServerHsOpcUaNodes.initNodeId);
        assertEquals(HandshakeCapability.ServerMessageTypes.OkResponseInitHandover.toString(), response);

        expectServerSideState(ServerSideStates.STARTING);
        expectServerSideState(ServerSideStates.PREPARING);
        expectServerSideState(ServerSideStates.READY_EMPTY);

        response = client().callStringMethodBlocking(ServerHsOpcUaNodes.startNodeId);
        assertEquals(HandshakeCapability.ServerMessageTypes.OkResponseStartHandover.toString(), response);
        expectServerSideState(ServerSideStates.EXECUTE);

        client().callStringMethodBlocking(ServerHsOpcUaNodes.completeNodeId);
        expectServerSideState(ServerSideStates.COMPLETING);
        expectServerSideState(ServerSideStates.COMPLETE);

        String state = client().readStringVariableNode(ServerHsOpcUaNodes.stateNodeId);
        assertEquals(ServerSideStates.COMPLETE.toString(), state);
    }

    private void finishHandshakeCycleLoaded() throws Exception{
        String response = client().callStringMethodBlocking(ServerHsOpcUaNodes.initNodeId);
        assertEquals(HandshakeCapability.ServerMessageTypes.OkResponseInitHandover.toString(), response);

        expectServerSideState(ServerSideStates.STARTING);
        expectServerSideState(ServerSideStates.PREPARING);
        expectServerSideState(ServerSideStates.READY_LOADED);

        response = client().callStringMethodBlocking(ServerHsOpcUaNodes.startNodeId);
        assertEquals(HandshakeCapability.ServerMessageTypes.OkResponseStartHandover.toString(), response);
        expectServerSideState(ServerSideStates.EXECUTE);

        client().callStringMethodBlocking(ServerHsOpcUaNodes.completeNodeId);
        expectServerSideState(ServerSideStates.COMPLETING);
        expectServerSideState(ServerSideStates.COMPLETE);

        String state = client().readStringVariableNode(ServerHsOpcUaNodes.stateNodeId);
        assertEquals(ServerSideStates.COMPLETE.toString(), state);
    }

    private void expectServerSideState(ServerSideStates serverSideState) {
        ServerHandshakeStatusUpdateEvent machineStatusUpdateEvent;
        machineStatusUpdateEvent = probe().expectMsgClass(Duration.ofSeconds(10), ServerHandshakeStatusUpdateEvent.class);
        assertEquals(serverSideState, machineStatusUpdateEvent.getStatus());
    }

    private ActorRef actorRef(){
        return infrastructure.getActorRef();
    }

    private TestKit probe(){
        return infrastructure.getProbe();
    }

    private FiabOpcUaClient client(){
        return infrastructure.getClient();
    }

    static class ServerHsOpcUaNodes {
        static final NodeId resetNodeId = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU_NORTH_SERVER/RESET");
        static final NodeId stopNodeId = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU_NORTH_SERVER/STOP");
        static final NodeId stateNodeId = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU_NORTH_SERVER/STATE");
        static final NodeId initNodeId = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU_NORTH_SERVER/INIT_HANDOVER");
        static final NodeId startNodeId = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU_NORTH_SERVER/START_HANDOVER");
        static final NodeId completeNodeId = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU_NORTH_SERVER/COMPLETE");
    }
}
