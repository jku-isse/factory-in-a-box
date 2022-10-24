package coordinator.opcua;

import akka.actor.ActorRef;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.functionalunit.ResetRequest;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.core.capabilities.handshake.client.CompleteHandshake;
import fiab.core.capabilities.wiring.WiringInfo;
import fiab.core.capabilities.wiring.WiringInfoBuilder;
import fiab.functionalunit.connector.FUConnector;
import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.handshake.client.messages.WiringRequest;
import fiab.handshake.server.messages.ServerHandshakeStatusUpdateEvent;
import fiab.handshake.server.opcua.functionalunit.ServerHandshakeFU;
import fiab.opcua.server.OPCUABase;
import fiab.turntable.infrastructure.OpcUaMachineChildFUs;
import fiab.turntable.opcua.OpcUaTurntableActor;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.junit.jupiter.api.*;
import testutils.FUTestInfrastructure;
import testutils.PortUtils;

import java.time.Duration;

import static fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("IntegrationTest")
public class TestTurntableOpcUa {

    private FUTestInfrastructure testFixture;
    private TestKit remoteHandshakeProbe;
    private IntraMachineEventBus remoteIntraMachineBus;
    private ActorRef serverHandshakeActor;
    private WiringInfo wiringInfoNorth;
    private WiringInfo wiringInfoEast;
    private WiringInfo wiringInfoSouth;
    private WiringInfo wiringInfoWest;

    //Playground
    public static void main(String[] args) {
        FUTestInfrastructure fixture = new FUTestInfrastructure(4840);
        OpcUaMachineChildFUs infrastructure = new OpcUaMachineChildFUs(fixture.getServer());
        fixture.initializeActor(OpcUaTurntableActor.props(fixture.getServer(),
                fixture.getServer().getRootNode(), "TestTurntable",
                fixture.getMachineEventBus(), fixture.getIntraMachineEventBus(), infrastructure), "TestTurntable");
        OPCUABase base = OPCUABase.createAndStartLocalServer(4841, "RemoteDevice");
        fixture.getSystem().actorOf(ServerHandshakeFU.props(base, base.getRootNode(),
                "RemoteHandshakeServer", new FUConnector(), fixture.getIntraMachineEventBus()));
    }

    @BeforeEach
    public void setup() {
        testFixture = new FUTestInfrastructure(PortUtils.findNextFreePort());
        int port = testFixture.getPort() + testFixture.getAndIncrementRunCount() + 1; //Skip 4840 since this is the port of the Turntable
        remoteIntraMachineBus = new IntraMachineEventBus();
        OPCUABase base = OPCUABase.createAndStartLocalServer(port, "RemoteDevice");
        serverHandshakeActor = testFixture.getSystem().actorOf(ServerHandshakeFU.props(base, base.getRootNode(),
                "RemoteHandshakeServer", new FUConnector(), remoteIntraMachineBus));
        remoteHandshakeProbe = new TestKit(testFixture.getSystem());
        remoteIntraMachineBus.subscribe(remoteHandshakeProbe.getRef(), testFixture.getTestClassifier());

        wiringInfoNorth = createServerHandshakeWiringInfo(TRANSPORT_MODULE_NORTH_CLIENT, port);
        wiringInfoEast = createServerHandshakeWiringInfo(TRANSPORT_MODULE_EAST_CLIENT, port);
        wiringInfoSouth = createServerHandshakeWiringInfo(TRANSPORT_MODULE_SOUTH_CLIENT, port);
        wiringInfoWest = createServerHandshakeWiringInfo(TRANSPORT_MODULE_WEST_CLIENT, port);

        testFixture.getMachineEventBus().subscribe(testFixture.getProbe().getRef(), testFixture.getTestClassifier());
        OpcUaMachineChildFUs ttComponents = new OpcUaMachineChildFUs(testFixture.getServer());
        testFixture.initializeActor(OpcUaTurntableActor.props(testFixture.getServer(),
                testFixture.getServer().getRootNode(), "TestTurntable",
                testFixture.getMachineEventBus(), testFixture.getIntraMachineEventBus(), ttComponents), "TestTurntable");
        expectCoordinatorState(BasicMachineStates.STOPPED);
        testFixture.connectClient();
    }

    @AfterEach
    public void teardown() {
        testFixture.shutdownInfrastructure();
    }

    @Test
    public void testResetTurntable() {
        assertDoesNotThrow(() -> {
            testFixture.getClient().callStringMethodBlocking(TurntableOpcUaNodes.resetNode);
            expectCoordinatorState(BasicMachineStates.RESETTING);
            expectCoordinatorState(BasicMachineStates.IDLE);
        });
    }

    @Test
    public void testTurntableReachesExecute(){
        actor().tell(new WiringRequest(testFixture.eventSourceId, wiringInfoEast), ActorRef.noSender());
        actor().tell(new WiringRequest(testFixture.eventSourceId, wiringInfoWest), ActorRef.noSender());
        assertDoesNotThrow(() -> {
            testFixture.getClient().callStringMethodBlocking(TurntableOpcUaNodes.resetNode);
            expectCoordinatorState(BasicMachineStates.RESETTING);
            expectCoordinatorState(BasicMachineStates.IDLE);
            testFixture.getClient().callStringMethodBlocking(TurntableOpcUaNodes.transportReq,
                    TRANSPORT_MODULE_WEST_CLIENT, TRANSPORT_MODULE_EAST_CLIENT, "order-1", "1");
            expectCoordinatorState(BasicMachineStates.STARTING);
            expectCoordinatorState(BasicMachineStates.EXECUTE);
        });
    }

    @Test
    public void testTurntableTransportFromServerToServerSuccessful(){
        assertDoesNotThrow(() -> {
            testFixture.getClient().callStringMethodBlocking(TurntableOpcUaNodes.resetNode);
            expectCoordinatorState(BasicMachineStates.RESETTING);
            expectCoordinatorState(BasicMachineStates.IDLE);
            testFixture.getClient().callStringMethodBlocking(TurntableOpcUaNodes.transportReq,
                    TRANSPORT_MODULE_NORTH_SERVER, TRANSPORT_MODULE_SOUTH_SERVER, "order-1", "1");
            expectCoordinatorState(BasicMachineStates.STARTING);
            expectCoordinatorState(BasicMachineStates.EXECUTE);

            testFixture.getClient().callStringMethod(TurntableNorthHandshakeOpcUaNodes.initHandover);
            testFixture.getClient().callStringMethod(TurntableNorthHandshakeOpcUaNodes.startHandover);
            while(!testFixture.getClient().readStringVariableNode(TurntableSouthHandshakeOpcUaNodes.statusNode).startsWith("IDLE")){
                //Wait for server hs fu to reach idle or find a better way to implement this (fishForMessage?)
            }
            testFixture.getClient().callStringMethod(TurntableSouthHandshakeOpcUaNodes.initHandover);
            testFixture.getClient().callStringMethod(TurntableSouthHandshakeOpcUaNodes.startHandover);

            expectCoordinatorState(BasicMachineStates.COMPLETING);
            expectCoordinatorState(BasicMachineStates.COMPLETE);
        });
    }

    @Test
    public void testTurntableTransportFromServerToClientSuccessful(){
        actor().tell(new WiringRequest(testFixture.eventSourceId, wiringInfoEast), ActorRef.noSender());
        assertDoesNotThrow(() -> {
            testFixture.getClient().callStringMethodBlocking(TurntableOpcUaNodes.resetNode);
            expectCoordinatorState(BasicMachineStates.RESETTING);
            expectCoordinatorState(BasicMachineStates.IDLE);
            testFixture.getClient().callStringMethodBlocking(TurntableOpcUaNodes.transportReq,
                    TRANSPORT_MODULE_NORTH_SERVER, TRANSPORT_MODULE_EAST_CLIENT, "order-1", "1");
            expectCoordinatorState(BasicMachineStates.STARTING);
            expectCoordinatorState(BasicMachineStates.EXECUTE);

            testFixture.getClient().callStringMethod(TurntableNorthHandshakeOpcUaNodes.initHandover);
            testFixture.getClient().callStringMethod(TurntableNorthHandshakeOpcUaNodes.startHandover);

            serverHandshakeActor.tell(new ResetRequest(testFixture.eventSourceId), ActorRef.noSender());
            expectCoordinatorState(BasicMachineStates.COMPLETING);
            expectCoordinatorState(BasicMachineStates.COMPLETE);
            serverHandshakeActor.tell(new CompleteHandshake(testFixture.eventSourceId), ActorRef.noSender());
        });
    }

    @Test
    public void testTurntableTransportFromClientToServerSuccessful(){
        actor().tell(new WiringRequest(testFixture.eventSourceId, wiringInfoWest), ActorRef.noSender());
        assertDoesNotThrow(() -> {
            testFixture.getClient().callStringMethodBlocking(TurntableOpcUaNodes.resetNode);
            expectCoordinatorState(BasicMachineStates.RESETTING);
            expectCoordinatorState(BasicMachineStates.IDLE);
            testFixture.getClient().callStringMethodBlocking(TurntableOpcUaNodes.transportReq,
                    TRANSPORT_MODULE_WEST_CLIENT, TRANSPORT_MODULE_SOUTH_SERVER, "order-1", "1");
            expectCoordinatorState(BasicMachineStates.STARTING);
            expectCoordinatorState(BasicMachineStates.EXECUTE);

            serverHandshakeActor.tell(new ResetRequest(testFixture.eventSourceId), ActorRef.noSender());
            remoteHandshakeProbe.fishForMessage(Duration.ofSeconds(10), "Expect remote hs to reach executing", msg -> {
                if(msg instanceof ServerHandshakeStatusUpdateEvent){
                    return ((ServerHandshakeStatusUpdateEvent) msg).getStatus() == ServerSideStates.EXECUTE;
                }
                return false;
            });
            while(!testFixture.getClient().readStringVariableNode(TurntableSouthHandshakeOpcUaNodes.statusNode).startsWith("IDLE")){
                //Wait for server hs fu to reach idle or find a better way to implement this (fishForMessage?)
            }
            testFixture.getClient().callStringMethod(TurntableSouthHandshakeOpcUaNodes.initHandover);
            testFixture.getClient().callStringMethod(TurntableSouthHandshakeOpcUaNodes.startHandover);

            expectCoordinatorState(BasicMachineStates.COMPLETING);
            expectCoordinatorState(BasicMachineStates.COMPLETE);
            serverHandshakeActor.tell(new CompleteHandshake(testFixture.eventSourceId), ActorRef.noSender());
        });
    }

    @Test
    public void testTurntableTransportFromClientToClientSuccessful(){
        actor().tell(new WiringRequest(testFixture.eventSourceId, wiringInfoEast), ActorRef.noSender());
        actor().tell(new WiringRequest(testFixture.eventSourceId, wiringInfoWest), ActorRef.noSender());
        assertDoesNotThrow(() -> {
            testFixture.getClient().callStringMethodBlocking(TurntableOpcUaNodes.resetNode);
            expectCoordinatorState(BasicMachineStates.RESETTING);
            expectCoordinatorState(BasicMachineStates.IDLE);
            testFixture.getClient().callStringMethodBlocking(TurntableOpcUaNodes.transportReq,
                    TRANSPORT_MODULE_WEST_CLIENT, TRANSPORT_MODULE_EAST_CLIENT, "order-1", "1");
            expectCoordinatorState(BasicMachineStates.STARTING);
            expectCoordinatorState(BasicMachineStates.EXECUTE);
            serverHandshakeActor.tell(new ResetRequest(testFixture.eventSourceId), ActorRef.noSender());
            remoteHandshakeProbe.fishForMessage(Duration.ofSeconds(10), "Expect remote hs to reach executing", msg -> {
                if(msg instanceof ServerHandshakeStatusUpdateEvent){
                    return ((ServerHandshakeStatusUpdateEvent) msg).getStatus() == ServerSideStates.EXECUTE;
                }
                return false;
            });
            serverHandshakeActor.tell(new CompleteHandshake(testFixture.eventSourceId), ActorRef.noSender());
            remoteHandshakeProbe.expectMsgClass(ServerHandshakeStatusUpdateEvent.class);    //Completing
            remoteHandshakeProbe.expectMsgClass(ServerHandshakeStatusUpdateEvent.class);    //Complete
            serverHandshakeActor.tell(new ResetRequest(testFixture.eventSourceId), ActorRef.noSender());
            expectCoordinatorState(BasicMachineStates.COMPLETING);
            expectCoordinatorState(BasicMachineStates.COMPLETE);
            serverHandshakeActor.tell(new CompleteHandshake(testFixture.eventSourceId), ActorRef.noSender());
        });
    }

    private ActorRef actor(){
        return testFixture.getActorRef();
    }

    private void expectCoordinatorState(BasicMachineStates coordinatorState) {
        MachineStatusUpdateEvent machineStatusUpdateEvent = testFixture.getProbe().expectMsgClass(Duration.ofSeconds(10), MachineStatusUpdateEvent.class);
        assertEquals(coordinatorState, machineStatusUpdateEvent.getStatus());
    }

    private static WiringInfo createServerHandshakeWiringInfo(String localCapabilityId, int port) {
        return new WiringInfoBuilder()
                .setLocalCapabilityId(localCapabilityId)
                .setRemoteCapabilityId("RemoteHandshakeServer")
                .setRemoteEndpointURL("opc.tcp://127.0.0.1:" + (port))
                .setRemoteNodeId("ns=2;s=RemoteDevice/HANDSHAKE_FU_RemoteHandshakeServer/CAPABILITIES/CAPABILITY")
                .setRemoteRole("RemoteRole1")
                .build();
    }

    static class TurntableOpcUaNodes {
        public static NodeId resetNode = NodeId.parse("ns=2;s=TestDevice/Reset");
        public static NodeId stopNode = NodeId.parse("ns=2;s=TestDevice/Stop");
        public static NodeId transportReq = NodeId.parse("ns=2;s=TestDevice/TransportRequest");
        public static NodeId statusNode = NodeId.parse("ns=2;s=TestDevice/STATE");
    }

    static class TurntableNorthHandshakeOpcUaNodes {
        public static NodeId resetNode = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU_NORTH_SERVER/RESET");
        public static NodeId stopNode = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU_NORTH_SERVER/STOP");
        public static NodeId initHandover = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU_NORTH_SERVER/INIT_HANDOVER");
        public static NodeId startHandover = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU_NORTH_SERVER/START_HANDOVER");
        public static NodeId statusNode = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU_NORTH_SERVER/STATE");
    }

    static class TurntableSouthHandshakeOpcUaNodes{
        public static NodeId resetNode = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU_SOUTH_SERVER/RESET");
        public static NodeId stopNode = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU_SOUTH_SERVER/STOP");
        public static NodeId initHandover = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU_SOUTH_SERVER/INIT_HANDOVER");
        public static NodeId startHandover = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU_SOUTH_SERVER/START_HANDOVER");
        public static NodeId statusNode = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU_SOUTH_SERVER/STATE");
    }
}
