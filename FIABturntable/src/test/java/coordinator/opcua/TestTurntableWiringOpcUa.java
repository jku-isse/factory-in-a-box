package coordinator.opcua;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.handshake.server.opcua.functionalunit.ServerHandshakeFU;
import fiab.opcua.client.FiabOpcUaClient;
import fiab.opcua.client.OPCUAClientFactory;
import fiab.opcua.server.OPCUABase;
import fiab.turntable.opcua.OpcUaTurntableActor;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.junit.jupiter.api.*;

import static fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("IntegrationTest")
public class TestTurntableWiringOpcUa {

    private static ActorSystem system;

    private OPCUABase hsBase;
    private ActorRef remoteHandshakeNorth;
    private ActorRef remoteHandshakeSouth;
    private ActorRef remoteHandshakeEast;
    private ActorRef remoteHandshakeWest;

    private OPCUABase ttBase;
    private ActorRef turntable;

    @BeforeAll
    public static void init() {
        system = ActorSystem.create();
    }

    @BeforeEach
    public void setup() {
        ttBase = OPCUABase.createAndStartLocalServer(4840, "VirtualTurntable");
        hsBase = OPCUABase.createAndStartLocalServer(4841, "HandshakeDevice");

        remoteHandshakeNorth = system.actorOf(ServerHandshakeFU.propsForStandaloneFU(hsBase, hsBase.getRootNode(), TRANSPORT_MODULE_NORTH_SERVER));
        remoteHandshakeEast = system.actorOf(ServerHandshakeFU.propsForStandaloneFU(hsBase, hsBase.getRootNode(), TRANSPORT_MODULE_EAST_SERVER));
        remoteHandshakeSouth = system.actorOf(ServerHandshakeFU.propsForStandaloneFU(hsBase, hsBase.getRootNode(), TRANSPORT_MODULE_SOUTH_SERVER));
        remoteHandshakeWest = system.actorOf(ServerHandshakeFU.propsForStandaloneFU(hsBase, hsBase.getRootNode(), TRANSPORT_MODULE_WEST_SERVER));

        turntable = system.actorOf(OpcUaTurntableActor.propsForStandaloneTurntable(ttBase, ttBase.getRootNode(), "VirtualTurntable"), "Turntable");
    }

    @AfterEach
    public void teardown() {
        hsBase.shutDownOpcUaBase();
        ttBase.shutDownOpcUaBase();

        remoteHandshakeNorth.tell(PoisonPill.getInstance(), ActorRef.noSender());
        remoteHandshakeEast.tell(PoisonPill.getInstance(), ActorRef.noSender());
        remoteHandshakeSouth.tell(PoisonPill.getInstance(), ActorRef.noSender());
        remoteHandshakeWest.tell(PoisonPill.getInstance(), ActorRef.noSender());

        turntable.tell(PoisonPill.getInstance(), ActorRef.noSender());
    }

    @AfterAll
    public static void cleanup() {
        system.terminate();
    }

    @Test
    public void testWiringFromFileSuccessful() {
        Assertions.assertDoesNotThrow(() -> {
            FiabOpcUaClient client = OPCUAClientFactory.createFIABClient("opc.tcp://127.0.0.1:4840");
            client.connectFIABClient().get();
            String prefix = HandshakeCapability.CLIENT_CAPABILITY_ID + "_";
            NodeId northHSRoot = client.getNodeIdForBrowseName(prefix + TRANSPORT_MODULE_NORTH_CLIENT);

            Assertions.assertNotNull(northHSRoot);

            NodeId wiringInfoFolder = client.getChildNodeByBrowseName(northHSRoot, "WIRING_INFO");
            NodeId localCapId = client.getChildNodeByBrowseName(wiringInfoFolder, "LOCAL_CAP_ID");
            NodeId remoteCapId = client.getChildNodeByBrowseName(wiringInfoFolder, "REMOTE_CAP_ID");
            NodeId remoteEndpoint = client.getChildNodeByBrowseName(wiringInfoFolder, "REMOTE_ENDPOINT_URL");
            NodeId remoteNodeId = client.getChildNodeByBrowseName(wiringInfoFolder, "REMOTE_NODE_ID");
            NodeId remoteRole = client.getChildNodeByBrowseName(wiringInfoFolder, "REMOTE_ROLE");

            assertEquals("NORTH_CLIENT", client.readStringVariableNode(localCapId));
            assertEquals("NORTH_SERVER", client.readStringVariableNode(remoteCapId));
            assertEquals("opc.tcp://127.0.0.1:4841", client.readStringVariableNode(remoteEndpoint));
            assertEquals("ns=2;s=HandshakeDevice/HANDSHAKE_FU_NORTH_SERVER/CAPABILITIES/CAPABILITY", client.readStringVariableNode(remoteNodeId));
            assertEquals("RemoteRole1", client.readStringVariableNode(remoteRole));
        });
    }

    @Test
    @Disabled("Not implemented")
    public void testSaveWiringToFileSuccessful(){
        //TODO
    }

    @Test
    @Disabled("Not implemented")
    public void testApplyNewWiring(){
        //TODO
    }

    @Test
    @Disabled("Not implemented")
    public void testDeleteWiring(){
        //TODO
    }
}
