package fiab.handshake.client.opcua;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.handshake.client.CompleteHandshake;
import fiab.core.capabilities.handshake.server.InitiateHandoverRequest;
import fiab.core.capabilities.handshake.server.StartHandoverRequest;
import fiab.core.capabilities.handshake.server.SubscribeToUpdatesRequest;
import fiab.core.capabilities.handshake.server.UnsubscribeToUpdatesRequest;
import fiab.core.capabilities.wiring.WiringInfo;
import fiab.core.capabilities.wiring.WiringInfoBuilder;
import fiab.functionalunit.connector.FUConnector;
import fiab.functionalunit.connector.FUSubscriptionClassifier;
import fiab.handshake.client.messages.WiringRequest;
import fiab.handshake.client.opcua.client.ClientSpawnerMessages;
import fiab.handshake.client.opcua.client.OpcUaServerHandshakeProxy;
import fiab.handshake.connector.ServerNotificationConnector;
import fiab.handshake.connector.ServerResponseConnector;
import fiab.handshake.server.messages.ServerHandshakeResponseEvent;
import fiab.handshake.server.messages.ServerHandshakeStatusUpdateEvent;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.junit.jupiter.api.*;

import java.time.Duration;

@Tag("SystemTest")
public class TestRemoteHandshakeProxy {

    private static ActorSystem system;
    private FUConnector requestBus;
    private ServerResponseConnector responseConnector;
    private ServerNotificationConnector notificationConnector;
    private ActorRef proxy;
    private TestKit probe;
    private String probeId;
    private WiringInfo nodeIds;

    @BeforeAll
    public static void init() {
        system = ActorSystem.create();
    }

    @BeforeEach
    public void setup() {
        requestBus = new FUConnector();
        responseConnector = new ServerResponseConnector();
        notificationConnector = new ServerNotificationConnector();
        probe = new TestKit(system);
        probeId = probe.getRef().path().name();
        proxy = system.actorOf(OpcUaServerHandshakeProxy.props(probe.getRef(),
                requestBus, responseConnector, notificationConnector), "Proxy");
        FUSubscriptionClassifier classifier = new FUSubscriptionClassifier(probeId, "*");
        responseConnector.subscribe(probe.getRef(), classifier);
        notificationConnector.subscribe(probe.getRef(), classifier);
        nodeIds = createServerHandshakeWiringInfo();
    }

    @AfterEach
    public void teardown() {
        requestBus = null;
        responseConnector = null;
        notificationConnector = null;
        proxy.tell(PoisonPill.getInstance(), ActorRef.noSender());
        proxy = null;
    }

    @AfterAll
    public static void cleanup() {
        system.terminate();
    }

    @Test
    public void testRemoteHandshakeProxy(){
        runFullHandshakeSequence();
    }

    @Test
    public void testRemoteHandshakeProxy4diac() {
        nodeIds = createServerHandshakeWiringInfoForte();    //use 4diac nodes instead
        runFullHandshakeSequence();
    }

    @Test
    public void testRemoteHandshakeWaitsWithInitUntilServerIsReady4diac(){
        nodeIds = createServerHandshakeWiringInfoForte();    //use 4diac nodes instead
        proxy.tell(new WiringRequest(probeId,nodeIds), probe.getRef());
        probe.expectMsgClass(Duration.ofSeconds(15), ClientSpawnerMessages.ClientCreated.class);

        proxy.tell(new SubscribeToUpdatesRequest(probeId), probe.getRef());
        probe.expectMsgClass(ServerHandshakeStatusUpdateEvent.class);
        probe.expectNoMessage();
    }

    private void runFullHandshakeSequence(){
        new TestKit(system) {
            {
                proxy.tell(new WiringRequest(probeId,nodeIds), probe.getRef());
                probe.expectMsgClass(Duration.ofSeconds(15), ClientSpawnerMessages.ClientCreated.class);

                proxy.tell(new SubscribeToUpdatesRequest(probeId), probe.getRef());
                probe.expectMsgClass(ServerHandshakeStatusUpdateEvent.class);

                //Init handshake and wait for response + ready state
                proxy.tell(new InitiateHandoverRequest(probeId), probe.getRef());
                probe.expectMsgClass(Duration.ofSeconds(15),ServerHandshakeResponseEvent.class);
                probe.expectMsgClass(Duration.ofSeconds(15), ServerHandshakeStatusUpdateEvent.class);

                //Start handshake and wait for response + execute state
                proxy.tell(new StartHandoverRequest(probeId), probe.getRef());
                probe.expectMsgClass(Duration.ofSeconds(15), ServerHandshakeResponseEvent.class);
                probe.expectMsgClass(Duration.ofSeconds(15), ServerHandshakeStatusUpdateEvent.class);

                //Wait for completing
                probe.expectMsgClass(Duration.ofSeconds(15), ServerHandshakeStatusUpdateEvent.class);

                //Unsubscribe from remote state
                proxy.tell(new UnsubscribeToUpdatesRequest(probeId), probe.getRef());

                //Check if we unsubscribed to events
                probe.expectNoMessage();
            }
        };
    }

    //Requires the 4diac outputStation at pos 35 to be running
    private WiringInfo createServerHandshakeWiringInfoForte() {
        return new WiringInfoBuilder()
                .setLocalCapabilityId("NORTH_CLIENT")
                .setRemoteCapabilityId("DefaultHandshakeServerSide")
                .setRemoteEndpointURL("opc.tcp://192.168.0.35:4840")
                .setRemoteNodeId("ns=1;i=327")
                .setRemoteRole("Provided")
                .build();
    }

    //Requires a running server handshake FU instance running on localhost
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
