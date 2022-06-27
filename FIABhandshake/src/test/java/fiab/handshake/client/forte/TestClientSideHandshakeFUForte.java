package fiab.handshake.client.forte;

import akka.actor.ActorRef;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.handshake.ClientSideStates;
import fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers;
import fiab.core.capabilities.wiring.WiringInfo;
import fiab.core.capabilities.wiring.WiringInfoBuilder;
import fiab.functionalunit.connector.FUConnector;
import fiab.handshake.client.messages.ClientHandshakeStatusUpdateEvent;
import fiab.handshake.client.messages.WiringRequest;
import fiab.handshake.client.opcua.functionalunit.ClientHandshakeFU;
import fiab.handshake.connector.ServerNotificationConnector;
import fiab.handshake.connector.ServerResponseConnector;
import fiab.opcua.client.FiabOpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.junit.jupiter.api.*;
import testutils.FUTestInfrastructure;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * These tests should be run against server handshake implementations in 4diac
 * Currently the target machine is the OutputStation
 * To change the machine, change the config files on the bottom of this file
 */
@Tag("SystemTest")
public class TestClientSideHandshakeFUForte {

    private static FUTestInfrastructure infrastructure;
    private static FUConnector clientConnector;                         //Send messages to client hs

    private static FUConnector remoteFUConnector;                       //Client sends requests here
    private static ServerResponseConnector responseConnector;           //Server responses go here
    private static ServerNotificationConnector notificationConnector;   //Server status updates go here

    private WiringInfo wiringInfo;

    public static void main(String[] args) {
        FUTestInfrastructure testInfrastructure = new FUTestInfrastructure(4840);
        FUConnector clientConnector = new FUConnector();
        FUConnector remoteFUConnector = new FUConnector();
        ServerResponseConnector responseConnector = new ServerResponseConnector();
        ServerNotificationConnector notificationConnector = new ServerNotificationConnector();

        testInfrastructure.initializeActor(ClientHandshakeFU.props(
                        testInfrastructure.getServer(), testInfrastructure.getServer().getRootNode(),
                        TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_NORTH_CLIENT,
                        clientConnector, testInfrastructure.getIntraMachineEventBus(), remoteFUConnector,
                        responseConnector, notificationConnector),
                "ClientHandshakeFU" + testInfrastructure.getAndIncrementRunCount());
        testInfrastructure.getActorRef().tell(createServerHandshakeWiringInfoForte(), ActorRef.noSender());
    }

    @BeforeAll
    public static void setup() {
        infrastructure = new FUTestInfrastructure(4844);
        infrastructure.subscribeToIntraMachineEventBus();
    }

    @BeforeEach
    public void init() {
        infrastructure.connectClient();

        clientConnector = new FUConnector();
        remoteFUConnector = new FUConnector();
        responseConnector = new ServerResponseConnector();
        notificationConnector = new ServerNotificationConnector();

        infrastructure.initializeActor(ClientHandshakeFU.props(
                        infrastructure.getServer(), infrastructure.getServer().getRootNode(),
                        TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_NORTH_CLIENT,
                        clientConnector, infrastructure.getIntraMachineEventBus(), remoteFUConnector,
                        responseConnector, notificationConnector),
                "ClientHandshakeFU" + infrastructure.getAndIncrementRunCount());

        wiringInfo = createServerHandshakeWiringInfoForte();
        actorRef().tell(new WiringRequest(infrastructure.eventSourceId, wiringInfo), probe().getRef());
    }

    @AfterEach
    public void teardown() {
        infrastructure.destroyActor();
        infrastructure.disconnectClient();
    }

    @AfterAll
    public static void cleanup() {
        infrastructure.shutdownInfrastructure();
    }

    @Test
    public void testResetSuccess() {
        assertDoesNotThrow(() -> {
            String state = client().readStringVariableNode(ClientHsOpcUaNodes.stateNodeId);
            assertEquals(ClientSideStates.STOPPED.toString(), state);

            resetHandshakeClientForTest();
        });
    }

    @Test
    public void testFullHandshakeForte() {
        assertDoesNotThrow(() -> {
            String state = client().readStringVariableNode(ClientHsOpcUaNodes.stateNodeId);
            assertEquals(ClientSideStates.STOPPED.toString(), state);
            resetHandshakeClientForTest();

            client().callStringMethodBlocking(ClientHsOpcUaNodes.startNodeId);
            expectClientSideState(ClientSideStates.STARTING);
            expectClientSideState(ClientSideStates.INITIATING);
            expectClientSideState(ClientSideStates.INITIATED);
            expectClientSideState(ClientSideStates.READY);
            expectClientSideState(ClientSideStates.EXECUTE);

            client().callStringMethodBlocking(ClientHsOpcUaNodes.completeNodeId);
            expectClientSideState(ClientSideStates.COMPLETING);
            expectClientSideState(ClientSideStates.COMPLETED);
        });
    }

    private ActorRef actorRef() {
        return infrastructure.getActorRef();
    }

    private TestKit probe() {
        return infrastructure.getProbe();
    }

    private FiabOpcUaClient client() {
        return infrastructure.getClient();
    }

    private void resetHandshakeClientForTest() throws Exception {
        //We need to reset first
        client().callStringMethodBlocking(ClientHsOpcUaNodes.resetNodeId);
        expectClientSideState(ClientSideStates.RESETTING);
        expectClientSideState(ClientSideStates.IDLE);
        String state = client().readStringVariableNode(ClientHsOpcUaNodes.stateNodeId);
        assertEquals(ClientSideStates.IDLE.toString(), state);
    }

    private void expectClientSideState(ClientSideStates clientSideState) {
        ClientHandshakeStatusUpdateEvent machineStatusUpdateEvent;
        machineStatusUpdateEvent = probe().expectMsgClass(Duration.ofSeconds(10), ClientHandshakeStatusUpdateEvent.class);
        assertEquals(machineStatusUpdateEvent.getStatus(), clientSideState);
    }

    private static WiringInfo createServerHandshakeWiringInfoForte() {
        return new WiringInfoBuilder()
                .setLocalCapabilityId("NORTH_CLIENT")
                .setRemoteCapabilityId("DefaultHandshakeServerSide")
                .setRemoteEndpointURL("opc.tcp://192.168.0.35:4840")    //Make sure your pc is in the correct network!
                .setRemoteNodeId("ns=1;i=327")
                .setRemoteRole("Provided")
                .build();
    }

    static class ClientHsOpcUaNodes {
        static final NodeId resetNodeId = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU_NORTH_CLIENT/RESET");
        static final NodeId stopNodeId = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU_NORTH_CLIENT/STOP");
        static final NodeId stateNodeId = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU_NORTH_CLIENT/STATE");
        static final NodeId startNodeId = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU_NORTH_CLIENT/START");
        static final NodeId completeNodeId = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU_NORTH_CLIENT/COMPLETE");
    }
}
