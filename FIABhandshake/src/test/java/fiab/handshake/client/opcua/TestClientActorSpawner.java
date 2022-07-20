package fiab.handshake.client.opcua;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers;
import fiab.core.capabilities.wiring.WiringInfo;
import fiab.core.capabilities.wiring.WiringInfoBuilder;
import fiab.functionalunit.connector.FUConnector;
import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.handshake.client.opcua.client.ClientSpawnerActor;
import fiab.handshake.client.opcua.client.ClientSpawnerMessages;
import fiab.handshake.connector.ServerNotificationConnector;
import fiab.handshake.connector.ServerResponseConnector;
import fiab.handshake.server.opcua.functionalunit.ServerHandshakeFU;
import fiab.opcua.client.FiabOpcUaClient;
import fiab.opcua.server.OPCUABase;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@Tag("IntegrationTest")
public class TestClientActorSpawner {

    private ActorSystem system;
    private TestKit probe;
    private ActorRef spawner;
    private WiringInfo wiringInfo;
    private static OPCUABase serverOpcUaBase;

    @BeforeAll
    public static void init(){
        serverOpcUaBase = OPCUABase.createAndStartLocalServer(4840, "HandshakeServerDevice");
    }

    @BeforeEach
    public void setup() {
        system = ActorSystem.create();
        probe = new TestKit(system);
        system.actorOf(ServerHandshakeFU.propsForStandaloneFU(serverOpcUaBase, serverOpcUaBase.getRootNode()), "ServerHandshake");
        spawner = system.actorOf(ClientSpawnerActor.props(), "Spawner");
        wiringInfo = createServerHandshakeWiringInfo();
    }

    @AfterEach
    public void teardown() {
        TestKit.shutdownActorSystem(system);
    }

    @AfterAll
    public static void cleanup(){
        serverOpcUaBase.shutDownOpcUaBase();
    }

    @Test
    public void testActorSpawnerCreatesClient() {
        new TestKit(system) {
            {
                spawner.tell(new ClientSpawnerMessages.CreateNewClient(wiringInfo), probe.getRef());
                ClientSpawnerMessages.ClientCreated clientCreated;
                clientCreated = probe.expectMsgClass(Duration.ofSeconds(15), ClientSpawnerMessages.ClientCreated.class);
                FiabOpcUaClient client = clientCreated.getServerNodeIds().getClient();
                assertNotNull(client);
            }
        };
    }

    @Test
    public void testActorSpawnerClientCanReadVariable() {
        new TestKit(system) {
            {
                assertDoesNotThrow(() -> {
                    spawner.tell(new ClientSpawnerMessages.CreateNewClient(wiringInfo), probe.getRef());
                    ClientSpawnerMessages.ClientCreated clientCreated;
                    clientCreated = probe.expectMsgClass(Duration.ofSeconds(15), ClientSpawnerMessages.ClientCreated.class);
                    FiabOpcUaClient client = clientCreated.getServerNodeIds().getClient();
                    String result = client.readStringVariableNode(clientCreated.getServerNodeIds().getStateVar());
                    system.log().info(result);
                    assertFalse(result.isEmpty());
                });
            }
        };
    }

    @Test
    public void testActorSpawnerCanCancelClientCreation() {
        new TestKit(system) {
            {
                spawner.tell(new ClientSpawnerMessages.CreateNewClient(wiringInfo), probe.getRef());
                spawner.tell(new ClientSpawnerMessages.CancelClientCreation(), probe.getRef());
                probe.expectMsgClass(Duration.ofSeconds(15), ClientSpawnerMessages.ClientCreationCancelled.class);
            }
        };
    }

    @Test
    public void testActorSpawnerCanCancelClientCreationAfterTimeout() {
        new TestKit(system) {
            {
                spawner.tell(new ClientSpawnerMessages.CreateNewClient(wiringInfo), probe.getRef());
                system.scheduler()
                        //If this test passes try decreasing the delay before looking for other errors
                        .scheduleOnce(Duration.ofMillis(500),    //50 ms lets the spawner do something, but not complete
                                () -> spawner.tell(new ClientSpawnerMessages.CancelClientCreation(), probe.getRef()),
                                system.dispatcher());

                probe.expectMsgClass(Duration.ofSeconds(15), ClientSpawnerMessages.ClientCreationCancelled.class);
            }
        };
    }

    @Test
    public void testActorSpawnerRespondsWithFailMessageOnError() {
        new TestKit(system) {
            {
                WiringInfo emptyInfo = new WiringInfoBuilder().build();
                spawner.tell(new ClientSpawnerMessages.CreateNewClient(emptyInfo), probe.getRef());
                probe.expectMsgClass(Duration.ofSeconds(15), ClientSpawnerMessages.ClientCreationFailed.class);
            }
        };
    }

    /*private WiringInfo createServerHandshakeWiringInfoForte() {
        return new WiringInfoBuilder()
                .setLocalCapabilityId("NORTH_CLIENT")
                .setRemoteCapabilityId("DefaultHandshakeServerSide")
                .setRemoteEndpointURL("opc.tcp://192.168.0.35:4840")
                .setRemoteNodeId("ns=1;i=327")
                .setRemoteRole("Provided")
                .build();
    }*/

    private WiringInfo createServerHandshakeWiringInfo(){
        return new WiringInfoBuilder()
                .setLocalCapabilityId("NORTH_CLIENT")
                .setRemoteCapabilityId("DefaultHandshakeServerSide")
                .setRemoteEndpointURL("opc.tcp://0.0.0.0:4840")
                .setRemoteNodeId("ns=2;s=HandshakeServerDevice/HANDSHAKE_FU/CAPABILITIES/CAPABILITY")
                .setRemoteRole("Required")
                .build();
    }
}
