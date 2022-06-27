package fiab.handshake.fullHandshake.opcua;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.functionalunit.ResetRequest;
import fiab.core.capabilities.handshake.ClientSideStates;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.core.capabilities.handshake.client.CompleteHandshake;
import fiab.core.capabilities.handshake.client.PerformHandshake;
import fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers;
import fiab.core.capabilities.wiring.WiringInfo;
import fiab.core.capabilities.wiring.WiringInfoBuilder;
import fiab.functionalunit.connector.FUConnector;
import fiab.functionalunit.connector.FUSubscriptionClassifier;
import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.handshake.client.messages.ClientHandshakeStatusUpdateEvent;
import fiab.handshake.client.messages.WiringRequest;
import fiab.handshake.client.messages.WiringUpdateNotification;
import fiab.handshake.client.opcua.functionalunit.ClientHandshakeFU;
import fiab.handshake.server.messages.ServerHandshakeStatusUpdateEvent;
import fiab.handshake.server.opcua.functionalunit.ServerHandshakeFU;
import fiab.opcua.client.FiabOpcUaClient;
import fiab.opcua.client.OPCUAClientFactory;
import fiab.opcua.server.OPCUABase;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("IntegrationTest")
public class TestHandshakeProtocolOpcUa {

    private static ActorSystem system;
    private static final AtomicInteger runCounter = new AtomicInteger(0);

    private IntraMachineEventBus serverIntraMachineBus;
    private FUConnector serverRequestBus;

    private IntraMachineEventBus clientIntraMachineBus;
    private FUConnector clientRequestBus;

    private String serverProbeId;
    private TestKit serverProbe;

    private String clientProbeId;
    private TestKit clientProbe;

    private OPCUABase serverOpcUaBase;
    private ActorRef serverActor;

    private OPCUABase clientOpcUaBase;
    private ActorRef clientActor;

    private WiringInfo wiringInfo;
    private WiringInfo invalidInfo;
    private int currentRunCount;

    //Playground
    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create();
        IntraMachineEventBus serverIntraMachineBus = new IntraMachineEventBus();
        FUConnector serverRequestBus = new FUConnector();

        IntraMachineEventBus clientIntraMachineBus = new IntraMachineEventBus();
        FUConnector clientRequestBus = new FUConnector();

        OPCUABase serverOpcUaBase = OPCUABase.createAndStartLocalServer(4840, "HandshakeServerDevice");
        system.actorOf(ServerHandshakeFU.props(serverOpcUaBase, serverOpcUaBase.getRootNode(),
                TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_NORTH_SERVER,
                serverRequestBus, serverIntraMachineBus), "ServerSide");

        OPCUABase clientOpcUaBase = OPCUABase.createAndStartLocalServer(4841, "HandshakeClientDevice");
        ActorRef clientHs = system.actorOf(ClientHandshakeFU.props(clientOpcUaBase, clientOpcUaBase.getRootNode(),
                TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_NORTH_CLIENT,
                clientRequestBus, clientIntraMachineBus), "ClientSide");
        clientHs.tell(new WiringRequest("Test", createServerHandshakeWiringInfo(0)), ActorRef.noSender());
    }

    @BeforeAll
    public static void init() {
        system = ActorSystem.create();
    }

    @BeforeEach
    public void setup() {
        serverIntraMachineBus = new IntraMachineEventBus();
        serverRequestBus = new FUConnector();

        clientIntraMachineBus = new IntraMachineEventBus();
        clientRequestBus = new FUConnector();
        this.currentRunCount = runCounter.get();
        setupServerClientStructure(this.currentRunCount);
        runCounter.addAndGet(2);
    }

    @AfterEach
    public void teardown() {
        serverActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
        clientActor.tell(PoisonPill.getInstance(), ActorRef.noSender());

        serverOpcUaBase.shutDownOpcUaBase();
        clientOpcUaBase.shutdown();
    }

    @AfterAll
    public static void cleanup() {
        system.terminate();
    }

    @Test
    public void testFullHandshakeCycleWithEmptyServer() {
        //FIXME this test passed only manually at least once
        clientRequestBus.publish(new WiringRequest(clientProbeId, wiringInfo));
        clientProbe.expectMsgClass(WiringUpdateNotification.class);
        executeFullHandshakeProtocolServer(true);
    }

    @Test
    public void testFullHandshakeCycleWithFullServer() {
        clientRequestBus.publish(new WiringRequest(clientProbeId, wiringInfo));
        clientProbe.expectMsgClass(WiringUpdateNotification.class);
        executeFullHandshakeProtocolServer(false);
    }

    @Test
    public void testMultipleFullHandshakeCyclesEmpty() {
        clientRequestBus.publish(new WiringRequest(clientProbeId, wiringInfo));
        clientProbe.expectMsgClass(WiringUpdateNotification.class);
        for (int i = 0; i < 3; i++) {
            executeFullHandshakeProtocolServer(true);
        }
    }

    @Test
    public void testMultipleFullHandshakeCyclesLoaded() {
        clientRequestBus.publish(new WiringRequest(clientProbeId, wiringInfo));
        clientProbe.expectMsgClass(WiringUpdateNotification.class);
        for (int i = 0; i < 3; i++) {
            executeFullHandshakeProtocolServer(false);
        }
    }

    @Test
    public void testMultipleHandshakeCyclesAlternatingEmptyLoaded() {
        clientRequestBus.publish(new WiringRequest(clientProbeId, wiringInfo));
        clientProbe.expectMsgClass(WiringUpdateNotification.class);
        for (int i = 0; i < 5; i++) {
            if (i % 2 == 0) {
                executeFullHandshakeProtocolServer(true);
            } else {
                executeFullHandshakeProtocolServer(false);
            }
        }
    }

    @Test
    public void testHandshakeProtocolEmptyViaOpcUaSuccessful() {
        clientRequestBus.publish(new WiringRequest(clientProbeId, wiringInfo));
        clientProbe.expectMsgClass(WiringUpdateNotification.class);
        assertDoesNotThrow(() -> {
            FiabOpcUaClient serverClient;   //Client to access server handshake nodes
            serverClient = OPCUAClientFactory.createFIABClient("opc.tcp://127.0.0.1:" + (4840 + currentRunCount));
            serverClient.connectFIABClient().get();

            FiabOpcUaClient clientClient;   //Client to access client handshake nodes
            clientClient = OPCUAClientFactory.createFIABClient("opc.tcp://127.0.0.1:" + (4841 + currentRunCount));
            clientClient.connectFIABClient().get();
            //We reset both handshake components
            clientClient.callStringMethodBlocking(ClientHsOpcUaNodes.resetNodeId);
            serverClient.callStringMethodBlocking(ServerHsOpcUaNodes.resetNodeId);
            //Wait for the client to reach idle
            expectClientSideState(ClientSideStates.RESETTING);
            expectClientSideState(ClientSideStates.IDLE);
            //Wait for the server to reach idle
            expectServerSideState(ServerSideStates.RESETTING);
            expectServerSideState(ServerSideStates.IDLE_EMPTY);
            //Call clientHs to perform handshake
            clientClient.callStringMethodBlocking(ClientHsOpcUaNodes.startNodeId);
            expectClientSideState(ClientSideStates.STARTING);
            expectClientSideState(ClientSideStates.INITIATING);
            expectClientSideState(ClientSideStates.INITIATED);
            expectClientSideState(ClientSideStates.READY);
            expectClientSideState(ClientSideStates.EXECUTE);
            //This should also trigger the serverSide
            expectServerSideState(ServerSideStates.STARTING);
            expectServerSideState(ServerSideStates.PREPARING);
            expectServerSideState(ServerSideStates.READY_EMPTY);
            expectServerSideState(ServerSideStates.EXECUTE);
            //Now we finish the server handshake
            serverClient.callStringMethod(ServerHsOpcUaNodes.completeNodeId);
            expectServerSideState(ServerSideStates.COMPLETING);
            expectServerSideState(ServerSideStates.COMPLETE);
            //Finally, we complete the client handshake
            clientClient.callStringMethod(ClientHsOpcUaNodes.completeNodeId);
            expectClientSideState(ClientSideStates.COMPLETING);
            expectClientSideState(ClientSideStates.COMPLETED);
            //Also check the status variable is updated in opc ua
            String finalStateUa = clientClient.readStringVariableNode(ClientHsOpcUaNodes.stateNodeId);
            assertEquals(ClientSideStates.COMPLETED.toString(), finalStateUa);
        });
    }

    @Test
    public void testHandshakeProtocolLoadedViaOpcUaSuccessful() {
        clientRequestBus.publish(new WiringRequest(clientProbeId, wiringInfo));
        clientProbe.expectMsgClass(WiringUpdateNotification.class);
        assertDoesNotThrow(() -> {
            FiabOpcUaClient serverClient;   //Client to access server handshake nodes
            serverClient = OPCUAClientFactory.createFIABClient("opc.tcp://127.0.0.1:" + (4840 + currentRunCount));
            serverClient.connectFIABClient().get();

            serverClient.callStringMethodBlocking(ServerHsOpcUaNodes.setLoadedId);

            FiabOpcUaClient clientClient;   //Client to access client handshake nodes
            clientClient = OPCUAClientFactory.createFIABClient("opc.tcp://127.0.0.1:" + (4841 + currentRunCount));
            clientClient.connectFIABClient().get();
            //We reset both handshake components
            clientClient.callStringMethodBlocking(ClientHsOpcUaNodes.resetNodeId);
            serverClient.callStringMethodBlocking(ServerHsOpcUaNodes.resetNodeId);
            //Wait for the client to reach idle
            expectClientSideState(ClientSideStates.RESETTING);
            expectClientSideState(ClientSideStates.IDLE);
            //Wait for the server to reach idle
            expectServerSideState(ServerSideStates.RESETTING);
            expectServerSideState(ServerSideStates.IDLE_LOADED);
            //Call clientHs to perform handshake
            clientClient.callStringMethodBlocking(ClientHsOpcUaNodes.startNodeId);
            expectClientSideState(ClientSideStates.STARTING);
            expectClientSideState(ClientSideStates.INITIATING);
            expectClientSideState(ClientSideStates.INITIATED);
            expectClientSideState(ClientSideStates.READY);
            expectClientSideState(ClientSideStates.EXECUTE);
            //This should also trigger the serverSide
            expectServerSideState(ServerSideStates.STARTING);
            expectServerSideState(ServerSideStates.PREPARING);
            expectServerSideState(ServerSideStates.READY_LOADED);
            expectServerSideState(ServerSideStates.EXECUTE);
            //Now we finish the server handshake
            serverClient.callStringMethod(ServerHsOpcUaNodes.completeNodeId);
            expectServerSideState(ServerSideStates.COMPLETING);
            expectServerSideState(ServerSideStates.COMPLETE);
            //Finally, we complete the client handshake
            clientClient.callStringMethod(ClientHsOpcUaNodes.completeNodeId);
            expectClientSideState(ClientSideStates.COMPLETING);
            expectClientSideState(ClientSideStates.COMPLETED);
            //Also check the status variable is updated in opc ua
            String finalStateUa = clientClient.readStringVariableNode(ClientHsOpcUaNodes.stateNodeId);
            assertEquals(ClientSideStates.COMPLETED.toString(), finalStateUa);
        });
    }

    @Test
    public void testClientHandshakeWaitsForServerToReset() {
        clientRequestBus.publish(new WiringRequest(clientProbeId, wiringInfo));
        clientProbe.expectMsgClass(WiringUpdateNotification.class);
        clientRequestBus.publish(new ResetRequest(clientProbeId));
        system.scheduler().scheduleOnce(Duration.ofSeconds(10), () -> {
            serverActor.tell(new ResetRequest(serverProbeId), serverProbe.getRef());
        }, system.dispatcher());
        expectClientSideState(ClientSideStates.RESETTING);
        expectClientSideState(ClientSideStates.IDLE);
    }

    @Test
    public void testClientHandshakeDoesNotReachIdleWhenServerOffline() {
        clientRequestBus.publish(new WiringRequest(clientProbeId, createInvalidEndpointServerHandshakeWiringInfo()));
        clientProbe.expectMsgClass(WiringUpdateNotification.class);
        clientRequestBus.publish(new ResetRequest(clientProbeId));
        expectClientSideState(ClientSideStates.RESETTING);
        expectClientSideState(ClientSideStates.STOPPING);
        expectClientSideState(ClientSideStates.STOPPED);
    }

    @Test
    public void testClientRewiringSuccessful() {
        assertDoesNotThrow(() -> {
            //Try connecting to server using invalid wiring
            clientRequestBus.publish(new WiringRequest(clientProbeId, invalidInfo));
            clientProbe.expectMsgClass(WiringUpdateNotification.class);
            clientRequestBus.publish(new ResetRequest(clientProbeId));
            expectClientSideState(ClientSideStates.RESETTING);
            expectClientSideState(ClientSideStates.STOPPING);
            expectClientSideState(ClientSideStates.STOPPED);
            //Then connect to server using correct wiring, now reset should work
            clientRequestBus.publish(new WiringRequest(clientProbeId, wiringInfo));
            clientProbe.expectMsgClass(WiringUpdateNotification.class);
            clientRequestBus.publish(new ResetRequest(clientProbeId));
            expectClientSideState(ClientSideStates.RESETTING);
            expectClientSideState(ClientSideStates.IDLE);
        });
    }

    @Test
    public void testRewiringViaOpcUaSuccessful() {
        assertDoesNotThrow(() -> {
            FiabOpcUaClient client;   //Client to access client handshake nodes
            client = OPCUAClientFactory.createFIABClient("opc.tcp://127.0.0.1:" + (4841 + currentRunCount));
            client.connectFIABClient().get();
            String response;
            //Try connecting to server using invalid wiring
            response = client.callStringMethodBlocking(ClientHsOpcUaNodes.wiringNodeId,
                    new Variant(invalidInfo.getLocalCapabilityId()), new Variant(invalidInfo.getRemoteCapabilityId()),
                    new Variant(invalidInfo.getRemoteEndpointURL()), new Variant(invalidInfo.getRemoteNodeId()),
                    new Variant(invalidInfo.getRemoteRole()));
            assertEquals("Ok", response);
            clientProbe.expectMsgClass(WiringUpdateNotification.class);

            client.callStringMethodBlocking(ClientHsOpcUaNodes.resetNodeId);
            expectClientSideState(ClientSideStates.RESETTING);
            expectClientSideState(ClientSideStates.STOPPING);
            expectClientSideState(ClientSideStates.STOPPED);
            //Then connect to server using correct wiring, now reset should work
            response = client.callStringMethodBlocking(ClientHsOpcUaNodes.wiringNodeId,
                    new Variant(wiringInfo.getLocalCapabilityId()), new Variant(wiringInfo.getRemoteCapabilityId()),
                    new Variant(wiringInfo.getRemoteEndpointURL()), new Variant(wiringInfo.getRemoteNodeId()),
                    new Variant(wiringInfo.getRemoteRole()));
            assertEquals("Ok", response);
            clientProbe.expectMsgClass(WiringUpdateNotification.class);

            client.callStringMethodBlocking(ClientHsOpcUaNodes.resetNodeId);
            expectClientSideState(ClientSideStates.RESETTING);
            expectClientSideState(ClientSideStates.IDLE);
        });
    }

    //Assumes correct wiring
    private void executeFullHandshakeProtocolServer(boolean isServerEmpty) {
        assertDoesNotThrow(() -> {
            FiabOpcUaClient serverClient;   //Client to access server handshake nodes
            serverClient = OPCUAClientFactory.createFIABClient("opc.tcp://127.0.0.1:" + (4840 + currentRunCount));
            serverClient.connectFIABClient().get();
            if (isServerEmpty) {
                serverClient.callStringMethodBlocking(ServerHsOpcUaNodes.setEmptyId);
            } else {
                serverClient.callStringMethodBlocking(ServerHsOpcUaNodes.setLoadedId);
            }
            serverClient.disconnect();
            ServerSideStates expectedIdleState = isServerEmpty ? ServerSideStates.IDLE_EMPTY : ServerSideStates.IDLE_LOADED;
            ServerSideStates expectedReadyState = isServerEmpty ? ServerSideStates.READY_EMPTY : ServerSideStates.READY_LOADED;
            clientRequestBus.publish(new ResetRequest(clientProbeId));
            expectClientSideState(ClientSideStates.RESETTING);
            expectClientSideState(ClientSideStates.IDLE);

            serverRequestBus.publish(new ResetRequest(serverProbeId));
            expectServerSideState(ServerSideStates.RESETTING);
            expectServerSideState(expectedIdleState);

            clientRequestBus.publish(new PerformHandshake(clientProbeId));
            expectClientSideState(ClientSideStates.STARTING);

            expectClientSideState(ClientSideStates.INITIATING);

            expectServerSideState(ServerSideStates.STARTING);
            expectServerSideState(ServerSideStates.PREPARING);
            expectServerSideState(expectedReadyState);

            expectClientSideState(ClientSideStates.INITIATED);
            expectClientSideState(ClientSideStates.READY);

            expectServerSideState(ServerSideStates.EXECUTE);

            expectClientSideState(ClientSideStates.EXECUTE);

            serverRequestBus.publish(new CompleteHandshake(serverProbeId));
            expectServerSideState(ServerSideStates.COMPLETING);
            expectServerSideState(ServerSideStates.COMPLETE);

            clientRequestBus.publish(new CompleteHandshake(clientProbeId));
            expectClientSideState(ClientSideStates.COMPLETING);
            expectClientSideState(ClientSideStates.COMPLETED);
        });
    }

    private void setupServerClientStructure(int portOffset) {
        wiringInfo = createServerHandshakeWiringInfo(portOffset);
        invalidInfo = createInvalidServerHandshakeWiringInfo(portOffset);
        setupServerStructure(portOffset);
        setupClientStructure(portOffset + 1);
    }

    private void setupServerStructure(int portOffset) {
        serverOpcUaBase = OPCUABase.createAndStartLocalServer(4840 + portOffset, "HandshakeServerDevice");
        serverProbe = new TestKit(system);
        serverProbeId = serverProbe.getRef().path().name();
        serverIntraMachineBus.subscribe(serverProbe.getRef(), new FUSubscriptionClassifier(serverProbeId, "*"));
        serverActor = system.actorOf(ServerHandshakeFU.props(serverOpcUaBase, serverOpcUaBase.getRootNode(),
                TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_NORTH_SERVER,
                serverRequestBus, serverIntraMachineBus), "ServerSide");
        expectServerSideState(ServerSideStates.STOPPED);
    }

    private void setupClientStructure(int portOffset) {
        clientOpcUaBase = OPCUABase.createAndStartLocalServer(4840 + portOffset, "HandshakeClientDevice");
        clientProbe = new TestKit(system);
        clientProbeId = clientProbe.getRef().path().name();
        clientIntraMachineBus.subscribe(clientProbe.getRef(), new FUSubscriptionClassifier(clientProbeId, "*"));
        clientActor = system.actorOf(ClientHandshakeFU.props(clientOpcUaBase, clientOpcUaBase.getRootNode(),
                TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_NORTH_CLIENT,
                clientRequestBus, clientIntraMachineBus), "ClientSide");
        expectClientSideState(ClientSideStates.STOPPED);
    }

    private void expectServerSideState(ServerSideStates state) {
        ServerHandshakeStatusUpdateEvent event;
        event = serverProbe.expectMsgClass(Duration.ofSeconds(15), ServerHandshakeStatusUpdateEvent.class);
        assertEquals(state, event.getStatus());
    }

    private void expectClientSideState(ClientSideStates state) {
        ClientHandshakeStatusUpdateEvent event;
        event = clientProbe.expectMsgClass(Duration.ofSeconds(15), ClientHandshakeStatusUpdateEvent.class);
        assertEquals(state, event.getStatus());
    }

    private static WiringInfo createInvalidServerHandshakeWiringInfo(int portOffset) {
        return new WiringInfoBuilder()
                .setLocalCapabilityId("WEST_CLIENT")
                .setRemoteCapabilityId("NonExistentHandshakeServerSide")
                .setRemoteEndpointURL("opc.tcp://127.0.0.1:" + (4840 + portOffset))
                .setRemoteNodeId("ns=2;s=Device/HANDSHAKE_FU_SERVER/CAPABILITIES/CAPABILITY")
                .setRemoteRole("RemoteRole1")
                .build();
    }

    private static WiringInfo createInvalidEndpointServerHandshakeWiringInfo() {
        return new WiringInfoBuilder()
                .setLocalCapabilityId("NORTH_CLIENT")
                .setRemoteCapabilityId("DefaultHandshakeServerSide")
                .setRemoteEndpointURL("opc.tcp://127.0.0.1:4800")
                .setRemoteNodeId("ns=2;s=HandshakeServerDevice/HANDSHAKE_FU_NORTH_SERVER/CAPABILITIES/CAPABILITY")
                .setRemoteRole("RemoteRole1")
                .build();
    }

    private static WiringInfo createServerHandshakeWiringInfo(int portOffset) {
        return new WiringInfoBuilder()
                .setLocalCapabilityId("NORTH_CLIENT")
                .setRemoteCapabilityId("DefaultHandshakeServerSide")
                .setRemoteEndpointURL("opc.tcp://127.0.0.1:" + (4840 + portOffset))
                .setRemoteNodeId("ns=2;s=HandshakeServerDevice/HANDSHAKE_FU_NORTH_SERVER/CAPABILITIES/CAPABILITY")
                .setRemoteRole("RemoteRole1")
                .build();
    }

    static class ServerHsOpcUaNodes {
        static final NodeId resetNodeId = NodeId.parse("ns=2;s=HandshakeServerDevice/HANDSHAKE_FU_NORTH_SERVER/RESET");
        static final NodeId stopNodeId = NodeId.parse("ns=2;s=HandshakeServerDevice/HANDSHAKE_FU_NORTH_SERVER/STOP");
        static final NodeId stateNodeId = NodeId.parse("ns=2;s=HandshakeServerDevice/HANDSHAKE_FU_NORTH_SERVER/STATE");
        static final NodeId initNodeId = NodeId.parse("ns=2;s=HandshakeServerDevice/HANDSHAKE_FU_NORTH_SERVER/INIT_HANDOVER");
        static final NodeId startNodeId = NodeId.parse("ns=2;s=HandshakeServerDevice/HANDSHAKE_FU_NORTH_SERVER/START_HANDOVER");
        static final NodeId completeNodeId = NodeId.parse("ns=2;s=HandshakeServerDevice/HANDSHAKE_FU_NORTH_SERVER/COMPLETE");
        static final NodeId setEmptyId = NodeId.parse("ns=2;s=HandshakeServerDevice/HANDSHAKE_FU_NORTH_SERVER/SetEmpty");
        static final NodeId setLoadedId = NodeId.parse("ns=2;s=HandshakeServerDevice/HANDSHAKE_FU_NORTH_SERVER/SetLoaded");
    }

    static class ClientHsOpcUaNodes {
        static final NodeId resetNodeId = NodeId.parse("ns=2;s=HandshakeClientDevice/HANDSHAKE_FU_NORTH_CLIENT/RESET");
        static final NodeId stopNodeId = NodeId.parse("ns=2;s=HandshakeClientDevice/HANDSHAKE_FU_NORTH_CLIENT/STOP");
        static final NodeId stateNodeId = NodeId.parse("ns=2;s=HandshakeClientDevice/HANDSHAKE_FU_NORTH_CLIENT/STATE");
        static final NodeId startNodeId = NodeId.parse("ns=2;s=HandshakeClientDevice/HANDSHAKE_FU_NORTH_CLIENT/START");
        static final NodeId completeNodeId = NodeId.parse("ns=2;s=HandshakeClientDevice/HANDSHAKE_FU_NORTH_CLIENT/COMPLETE");
        static final NodeId wiringNodeId = NodeId.parse("ns=2;s=HandshakeClientDevice/HANDSHAKE_FU_NORTH_CLIENT/SET_WIRING");   //TODO check
    }
}
