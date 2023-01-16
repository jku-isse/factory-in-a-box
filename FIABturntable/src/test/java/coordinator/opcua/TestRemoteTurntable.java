package coordinator.opcua;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.functionalunit.ResetRequest;
import fiab.core.capabilities.functionalunit.StopRequest;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.core.capabilities.handshake.client.CompleteHandshake;
import fiab.core.capabilities.wiring.WiringInfo;
import fiab.core.capabilities.wiring.WiringInfoBuilder;
import fiab.functionalunit.connector.FUConnector;
import fiab.functionalunit.connector.FUSubscriptionClassifier;
import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.handshake.client.util.WiringInfoOpcUaUtil;
import fiab.handshake.server.messages.ServerHandshakeStatusUpdateEvent;
import fiab.handshake.server.opcua.functionalunit.ServerHandshakeFU;
import fiab.opcua.client.FiabOpcUaClient;
import fiab.opcua.client.OPCUAClientFactory;
import fiab.opcua.server.OPCUABase;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import testutils.PortUtils;

import java.time.Duration;

import static fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers.*;
import static fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Tag("SystemTest")
public class TestRemoteTurntable {

    private final String testerEndpoint = "opc.tcp://192.168.133.113";
    private final String turntableEndpoint = "opc.tcp://192.168.133.93:4840";

    private ActorSystem system;
    private ActorRef mockServerHSActor;
    private TestKit hsMockProbe;
    private FiabOpcUaClient client;

    private WiringInfo wiringInfoNorth;
    private WiringInfo wiringInfoSouth;

    @BeforeEach
    public void setup() {
        try {
            system = ActorSystem.create("TestTurntableSystem");
            int port = PortUtils.findNextFreePort();
            IntraMachineEventBus hsMockIntraMachineEventBus = new IntraMachineEventBus();
            OPCUABase base = OPCUABase.createAndStartDiscoverableServer(port, "RemoteDevice");
            mockServerHSActor = system.actorOf(ServerHandshakeFU.props(base, base.getRootNode(),
                    "RemoteHandshakeServer", new FUConnector(), hsMockIntraMachineEventBus), "MockServerHs");
            hsMockProbe = new TestKit(system);
            hsMockIntraMachineEventBus.subscribe(hsMockProbe.getRef(), new FUSubscriptionClassifier("RemoteHsProbe", "*"));

            wiringInfoNorth = createServerHandshakeWiringInfo(TRANSPORT_MODULE_NORTH_CLIENT, testerEndpoint, port);
            wiringInfoSouth = createServerHandshakeWiringInfo(TRANSPORT_MODULE_SOUTH_CLIENT, testerEndpoint, port);

            client = OPCUAClientFactory.createFIABClientAndConnect(turntableEndpoint);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @AfterEach
    public void teardown() {
        TestKit.shutdownActorSystem(system);
    }

    @Test
    public void testTransportNorthServerHSToSouthServerHS() {
        assertDoesNotThrow(() -> {
            client.callStringMethodBlocking(TurntableOpcUaNodes.stopNode);
            waitForCoordinatorState(BasicMachineStates.STOPPED);
            client.callStringMethodBlocking(TurntableOpcUaNodes.resetNode);
            waitForCoordinatorState(BasicMachineStates.IDLE);
            client.callStringMethodBlocking(TurntableOpcUaNodes.transportReq,
                    TRANSPORT_MODULE_NORTH_SERVER, TRANSPORT_MODULE_SOUTH_SERVER, "order-1", "1");
            waitForCoordinatorState(BasicMachineStates.EXECUTE);

            while (!client.readStringVariableNode(TurntableNorthServerHandshakeOpcUaNodes.statusNode).startsWith("IDLE")) {
                //Wait for server hs fu to reach idle or find a better way to implement this (fishForMessage?)
            }
            client.callStringMethod(TurntableNorthServerHandshakeOpcUaNodes.initHandover);
            client.callStringMethod(TurntableNorthServerHandshakeOpcUaNodes.startHandover);
            while (!client.readStringVariableNode(TurntableSouthServerHandshakeOpcUaNodes.statusNode).startsWith("IDLE")) {
                //Wait for server hs fu to reach idle or find a better way to implement this (fishForMessage?)
            }
            client.callStringMethod(TurntableSouthServerHandshakeOpcUaNodes.initHandover);
            client.callStringMethod(TurntableSouthServerHandshakeOpcUaNodes.startHandover);

            waitForCoordinatorState(BasicMachineStates.COMPLETE);
        });
    }

    @Test
    public void testTransportNorthClientHSToSouthClientHS() {
        assertDoesNotThrow(() -> {
            client.callStringMethodBlocking(TurntableNorthClientHandshakeOpcUaNodes.setWiringNode,
                    WiringInfoOpcUaUtil.wiringInfoAsOpcUaArgs(wiringInfoNorth));
            client.callStringMethodBlocking(TurntableSouthClientHandshakeOpcUaNodes.setWiringNode,
                    WiringInfoOpcUaUtil.wiringInfoAsOpcUaArgs(wiringInfoSouth));

            mockServerHSActor.tell(new ResetRequest("tester"), hsMockProbe.getRef());

            client.callStringMethodBlocking(TurntableOpcUaNodes.stopNode);
            waitForCoordinatorState(BasicMachineStates.STOPPED);
            client.callStringMethodBlocking(TurntableOpcUaNodes.resetNode);
            waitForCoordinatorState(BasicMachineStates.IDLE);
            client.callStringMethodBlocking(TurntableOpcUaNodes.transportReq,
                    TRANSPORT_MODULE_NORTH_CLIENT, TRANSPORT_MODULE_SOUTH_CLIENT, "order-1", "1");
            waitForCoordinatorState(BasicMachineStates.EXECUTE);

            hsMockProbe.fishForMessage(Duration.ofSeconds(30), "Wait for first hs to reach execute",
                    msg -> ((ServerHandshakeStatusUpdateEvent) msg).getStatus() == ServerSideStates.EXECUTE);
            mockServerHSActor.tell(new CompleteHandshake("tester"), hsMockProbe.getRef());
            hsMockProbe.fishForMessage(Duration.ofSeconds(30), "Wait for first hs to complete",
                    msg -> ((ServerHandshakeStatusUpdateEvent) msg).getStatus() == ServerSideStates.COMPLETE);

            mockServerHSActor.tell(new ResetRequest("tester"), hsMockProbe.getRef());

            hsMockProbe.fishForMessage(Duration.ofSeconds(30), "Wait for second hs to complete",
                    msg -> ((ServerHandshakeStatusUpdateEvent) msg).getStatus() == ServerSideStates.EXECUTE);
            mockServerHSActor.tell(new CompleteHandshake("tester"), hsMockProbe.getRef());

            waitForCoordinatorState(BasicMachineStates.COMPLETE);
        });
    }

    private void waitForCoordinatorState(BasicMachineStates expectedState) throws UaException {
        while (!client.readStringVariableNode(TurntableOpcUaNodes.statusNode).startsWith(expectedState.toString())) {
            //Wait for client to read the expected state from the state variable
        }
    }

    private static WiringInfo createServerHandshakeWiringInfo(String localCapabilityId, String endpoint, int port) {
        return new WiringInfoBuilder()
                .setLocalCapabilityId(localCapabilityId)
                .setRemoteCapabilityId("RemoteHandshakeServer")
                .setRemoteEndpointURL(endpoint + ":" + (port))
                .setRemoteNodeId("ns=2;s=RemoteDevice/HANDSHAKE_FU_RemoteHandshakeServer/CAPABILITIES/CAPABILITY")
                .setRemoteRole("Required")
                .build();
    }

    private static class TurntableOpcUaNodes {
        public static NodeId resetNode = NodeId.parse("ns=2;s=Turntable1/Reset");
        public static NodeId stopNode = NodeId.parse("ns=2;s=Turntable1/Stop");
        public static NodeId transportReq = NodeId.parse("ns=2;s=Turntable1/TransportRequest");
        public static NodeId statusNode = NodeId.parse("ns=2;s=Turntable1/STATE");
    }

    static class TurntableNorthServerHandshakeOpcUaNodes {
        public static NodeId resetNode = NodeId.parse("ns=2;s=Turntable1/HANDSHAKE_FU_NORTH_SERVER/RESET");
        public static NodeId stopNode = NodeId.parse("ns=2;s=Turntable1/HANDSHAKE_FU_NORTH_SERVER/STOP");
        public static NodeId initHandover = NodeId.parse("ns=2;s=Turntable1/HANDSHAKE_FU_NORTH_SERVER/INIT_HANDOVER");
        public static NodeId startHandover = NodeId.parse("ns=2;s=Turntable1/HANDSHAKE_FU_NORTH_SERVER/START_HANDOVER");
        public static NodeId statusNode = NodeId.parse("ns=2;s=Turntable1/HANDSHAKE_FU_NORTH_SERVER/STATE");
    }

    static class TurntableSouthServerHandshakeOpcUaNodes {
        public static NodeId resetNode = NodeId.parse("ns=2;s=Turntable1/HANDSHAKE_FU_SOUTH_SERVER/RESET");
        public static NodeId stopNode = NodeId.parse("ns=2;s=Turntable1/HANDSHAKE_FU_SOUTH_SERVER/STOP");
        public static NodeId initHandover = NodeId.parse("ns=2;s=Turntable1/HANDSHAKE_FU_SOUTH_SERVER/INIT_HANDOVER");
        public static NodeId startHandover = NodeId.parse("ns=2;s=Turntable1/HANDSHAKE_FU_SOUTH_SERVER/START_HANDOVER");
        public static NodeId statusNode = NodeId.parse("ns=2;s=Turntable1/HANDSHAKE_FU_SOUTH_SERVER/STATE");
    }

    static class TurntableNorthClientHandshakeOpcUaNodes {
        public static NodeId setWiringNode = NodeId.parse("ns=2;s=Turntable1/HANDSHAKE_FU_NORTH_CLIENT/SET_WIRING");
    }

    static class TurntableSouthClientHandshakeOpcUaNodes {
        public static NodeId setWiringNode = NodeId.parse("ns=2;s=Turntable1/HANDSHAKE_FU_SOUTH_CLIENT/SET_WIRING");
    }
}
