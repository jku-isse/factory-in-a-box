package fiab.handshake.client;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.handshake.ClientSideStates;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers;
import fiab.core.capabilities.wiring.WiringInfo;
import fiab.core.capabilities.wiring.WiringInfoBuilder;
import fiab.functionalunit.connector.FUConnector;
import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.handshake.client.messages.ClientHandshakeStatusUpdateEvent;
import fiab.handshake.client.messages.WiringRequest;
import fiab.handshake.client.messages.WiringUpdateNotification;
import fiab.handshake.client.opcua.RemoteServerHandshakeNodeIds;
import fiab.handshake.client.opcua.functionalunit.ClientHandshakeFU;
import fiab.handshake.connector.ServerNotificationConnector;
import fiab.handshake.connector.ServerResponseConnector;
//import fiab.handshake.opcua.actor.StandaloneHandshakeActor;
import fiab.handshake.server.messages.ServerHandshakeStatusUpdateEvent;
import fiab.handshake.server.opcua.functionalunit.ServerHandshakeFU;
import fiab.opcua.client.FiabOpcUaClient;
import fiab.opcua.client.OPCUAClientFactory;
import fiab.opcua.server.OPCUABase;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.junit.jupiter.api.*;
import testutils.FUTestInfrastructure;
import testutils.PortUtils;

import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("IntegrationTest")
public class TestClientSideHandshakeFU {

    private static FUTestInfrastructure infrastructure;
    private static FUConnector clientConnector;                         //Send messages to client hs

    private static FUConnector remoteFUConnector;                       //Client sends requests here
    private static ServerResponseConnector responseConnector;           //Server responses go here
    private static ServerNotificationConnector notificationConnector;   //Server status updates go here

    private WiringInfo wiringInfo;
    private RemoteServerHandshakeNodeIds remoteServerHandshakeNodeIds;
    //private int runCount;
    private int localPort;
    private int remotePort;

    public static void main(String[] args) {
        FUTestInfrastructure testInfrastructure = new FUTestInfrastructure(4840);
        new TestClientSideHandshakeFU()
                .startupRemoteHandshakeServer(testInfrastructure.getSystem(), createServerHandshakeNodes(4841), 4841);
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
        testInfrastructure.getActorRef().tell(new WiringRequest("", createServerHandshakeWiringInfo(4841)),
                ActorRef.noSender());

    }

    /*@BeforeAll
    public static void init() {
        infrastructure = new FUTestInfrastructure(4850);
        infrastructure.subscribeToIntraMachineEventBus();
    }*/

    @BeforeEach
    public void setup() throws IOException {
        localPort = PortUtils.findNextFreePort();
        infrastructure = new FUTestInfrastructure(localPort);
        infrastructure.subscribeToIntraMachineEventBus();
        //runCount = infrastructure.getAndIncrementRunCount();
        remotePort = PortUtils.findNextFreePort();
        remoteServerHandshakeNodeIds = createServerHandshakeNodes(remotePort);
        startupRemoteHandshakeServer(infrastructure.getSystem(), remoteServerHandshakeNodeIds, remotePort);

        clientConnector = new FUConnector();
        remoteFUConnector = new FUConnector();
        responseConnector = new ServerResponseConnector();
        notificationConnector = new ServerNotificationConnector();


        infrastructure.initializeActor(ClientHandshakeFU.props(
                        infrastructure.getServer(), infrastructure.getServer().getRootNode(),
                        TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_NORTH_CLIENT,
                        clientConnector, infrastructure.getIntraMachineEventBus(), remoteFUConnector,
                        responseConnector, notificationConnector),
                "ClientHandshakeFU");
        expectClientSideState(ClientSideStates.STOPPED);
        wiringInfo = createServerHandshakeWiringInfo(remotePort);

        actorRef().tell(new WiringRequest(infrastructure.eventSourceId, wiringInfo), probe().getRef());
        probe().expectMsgClass(WiringUpdateNotification.class);
        infrastructure.connectClient();
    }

    @AfterEach
    public void teardown() {
        infrastructure.destroyActor();
        infrastructure.shutdownServer();
        infrastructure.disconnectClient();
        infrastructure.shutdownInfrastructure();
    }

    /*@AfterAll
    public static void cleanup() {
        infrastructure.shutdownInfrastructure();
    }*/

    @Test
    public void testResetSuccess() {
        assertDoesNotThrow(() -> {
            String state = client().readStringVariableNode(TestClientSideHandshakeFU.ClientHsOpcUaNodes.stateNodeId);
            assertEquals(ClientSideStates.STOPPED.toString(), state);

            resetHandshakeClientForTest();
        });
    }

    @Test
    public void testFullHandshake() {
        assertDoesNotThrow(() -> {
            String state = client().readStringVariableNode(TestClientSideHandshakeFU.ClientHsOpcUaNodes.stateNodeId);
            assertEquals(ClientSideStates.STOPPED.toString(), state);
            resetHandshakeClientForTest();

            client().callStringMethodBlocking(TestClientSideHandshakeFU.ClientHsOpcUaNodes.startNodeId);
            expectClientSideState(ClientSideStates.STARTING);
            expectClientSideState(ClientSideStates.INITIATING);
            expectClientSideState(ClientSideStates.INITIATED);
            expectClientSideState(ClientSideStates.READY);
            expectClientSideState(ClientSideStates.EXECUTE);

            client().callStringMethodBlocking(TestClientSideHandshakeFU.ClientHsOpcUaNodes.completeNodeId);
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
        client().callStringMethodBlocking(TestClientSideHandshakeFU.ClientHsOpcUaNodes.resetNodeId);
        expectClientSideState(ClientSideStates.RESETTING);
        expectClientSideState(ClientSideStates.IDLE);
        String state = client().readStringVariableNode(TestClientSideHandshakeFU.ClientHsOpcUaNodes.stateNodeId);
        assertEquals(ClientSideStates.IDLE.toString(), state);
    }

    private void expectClientSideState(ClientSideStates clientSideState) {
        ClientHandshakeStatusUpdateEvent machineStatusUpdateEvent;
        machineStatusUpdateEvent = probe().expectMsgClass(Duration.ofSeconds(10), ClientHandshakeStatusUpdateEvent.class);
        assertEquals(clientSideState, machineStatusUpdateEvent.getStatus());
    }

    private static WiringInfo createServerHandshakeWiringInfo(int remotePort) {
        return new WiringInfoBuilder()
                .setLocalCapabilityId("NORTH_CLIENT")
                .setRemoteCapabilityId("DefaultHandshakeServerSide")
                .setRemoteEndpointURL("opc.tcp://127.0.0.1:" + (remotePort))
                .setRemoteNodeId("ns=2;s=ServerHandshakeMachine/HANDSHAKE_FU_DefaultServerSideHandshake/CAPABILITIES/CAPABILITY")
                .setRemoteRole("Required")
                .build();
    }

    private static RemoteServerHandshakeNodeIds createServerHandshakeNodes(int remotePort) {
        RemoteServerHandshakeNodeIds nodeIds = new RemoteServerHandshakeNodeIds();
        nodeIds.setEndpoint("opc.tcp://127.0.0.1:" + (remotePort));
        nodeIds.setActorNode(new NodeId(2, "ServerHandshakeMachine/HANDSHAKE_FU_DefaultServerSideHandshake"));
        nodeIds.setStateVar(new NodeId(2, "ServerHandshakeMachine/HANDSHAKE_FU_DefaultServerSideHandshake/STATE"));
        nodeIds.setInitMethod(new NodeId(2, "ServerHandshakeMachine/HANDSHAKE_FU_DefaultServerSideHandshake/INIT_HANDOVER"));
        nodeIds.setStartMethod(new NodeId(2, "ServerHandshakeMachine/HANDSHAKE_FU_DefaultServerSideHandshake/START_HANDOVER"));
        return nodeIds;
    }

    private void startupRemoteHandshakeServer(ActorSystem system, RemoteServerHandshakeNodeIds nodeIds, int remotePort) {
        try {
            new TestKit(system) {
                {
                    IntraMachineEventBus intraMachineEventBus = new IntraMachineEventBus();
                    TestKit probe = new TestKit(system);
                    intraMachineEventBus.subscribe(probe.getRef(), infrastructure.getTestClassifier());
                    OPCUABase opcuaBase = OPCUABase.createAndStartLocalServer(remotePort, "ServerHandshakeMachine");
                    system.actorOf(ServerHandshakeFU.props(opcuaBase, opcuaBase.getRootNode(), "DefaultServerSideHandshake",
                            new FUConnector(), intraMachineEventBus), "HandshakeActor");
                    probe.expectMsgAnyClassOf(Duration.ofSeconds(15), ServerHandshakeStatusUpdateEvent.class, ServerSideStates.class);

                    resetRemoteHandshake(nodeIds);
                }
            };
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void resetRemoteHandshake(RemoteServerHandshakeNodeIds nodeIds) throws Exception {
        FiabOpcUaClient client = OPCUAClientFactory.createFIABClient(nodeIds.getEndpoint());
        client.connectFIABClient().get();
        NodeId resetNode = client.getChildNodeByBrowseName(nodeIds.getActorNode(), "RESET");
        client.callStringMethod(resetNode);
        client.disconnect().get();
    }

    static class ClientHsOpcUaNodes {
        static final NodeId resetNodeId = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU_NORTH_CLIENT/RESET");
        static final NodeId stopNodeId = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU_NORTH_CLIENT/STOP");
        static final NodeId stateNodeId = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU_NORTH_CLIENT/STATE");
        static final NodeId startNodeId = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU_NORTH_CLIENT/START");
        static final NodeId completeNodeId = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU_NORTH_CLIENT/COMPLETE");
    }
}
