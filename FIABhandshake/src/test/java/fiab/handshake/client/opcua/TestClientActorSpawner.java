package fiab.handshake.client.opcua;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.wiring.WiringInfo;
import fiab.core.capabilities.wiring.WiringInfoBuilder;
import fiab.handshake.client.opcua.client.ClientSpawnerActor;
import fiab.handshake.client.opcua.client.ClientSpawnerMessages;
import fiab.opcua.client.FiabOpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@Tag("UnitTest")
public class TestClientActorSpawner {

    private static ActorSystem system;
    private static final AtomicInteger runCount = new AtomicInteger(0);
    private TestKit probe;
    private ActorRef spawner;
    private WiringInfo wiringInfo;

    @BeforeAll
    public static void init() {
        system = ActorSystem.create();
    }

    @BeforeEach
    public void setup() {
        probe = new TestKit(system);
        spawner = system.actorOf(ClientSpawnerActor.props(), "Spawner" + runCount.getAndIncrement());
        wiringInfo = createServerHandshakeWiringInfoForte();//createServerHandshakeWiringInfo();
    }

    @AfterAll
    public static void cleanup() {
        system.terminate();
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
                        .scheduleOnce(Duration.ofMillis(100),    //100 ms lets the spawner do something, but not complete
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

    private WiringInfo createServerHandshakeWiringInfoForte() {
        return new WiringInfoBuilder()
                .setLocalCapabilityId("NORTH_CLIENT")
                .setRemoteCapabilityId("DefaultHandshakeServerSide")
                .setRemoteEndpointURL("opc.tcp://192.168.0.35:4840")
                .setRemoteNodeId("ns=1;i=327")
                .setRemoteRole("Provided")
                .build();
    }

    private WiringInfo createServerHandshakeWiringInfo(){
        return new WiringInfoBuilder()
                .setLocalCapabilityId("NORTH_CLIENT")
                .setRemoteCapabilityId("DefaultHandshakeServerSide")
                .setRemoteEndpointURL("opc.tcp://127.0.0.1:4840/milo")
                .setRemoteNodeId("ns=2;s=Handshake/ServerHandshake/HANDSHAKE_FU_DefaultServerSideHandshake/CAPABILITIES/CAPABILITY")
                .setRemoteRole("RemoteRole1")
                .build();
    }
}
