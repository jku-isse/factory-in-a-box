package fiab.handshake.server.proxy;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.pattern.Patterns;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.core.capabilities.handshake.server.InitiateHandoverRequest;
import fiab.core.capabilities.handshake.server.StartHandoverRequest;
import fiab.functionalunit.connector.FUConnector;
import fiab.functionalunit.connector.FUSubscriptionClassifier;
import fiab.handshake.connector.ServerResponseConnector;
import fiab.handshake.server.ClientProxyActor;
import fiab.handshake.server.messages.ServerHandshakeResponseEvent;
import org.junit.jupiter.api.*;

@Tag("UnitTest")
public class TestClientProxyActor {

    private static ActorSystem system;

    private TestKit probe;
    private String probeId;
    private ActorRef proxyActor;
    private ServerResponseConnector responseConnector;

    @BeforeAll
    public static void init() {
        system = ActorSystem.create();
    }

    @BeforeEach
    public void setup(){
        probe = new TestKit(system);
        probeId = probe.getRef().path().name();
        FUConnector requestConnector = new FUConnector();
        responseConnector = new ServerResponseConnector();
        requestConnector.subscribe(probe.getRef(), new FUSubscriptionClassifier(probeId, "*"));
        responseConnector.subscribe(probe.getRef(), new FUSubscriptionClassifier(probeId, "*"));
        proxyActor = system.actorOf(ClientProxyActor.props(requestConnector, responseConnector));
    }

    @AfterEach
    public void teardown(){
        proxyActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
    }

    @AfterAll
    public static void cleanup(){
        system.terminate();
    }

    @Test
    public void testProxyForwardsInit() {
        new TestKit(system) {
            {
                proxyActor.tell(new InitiateHandoverRequest(proxyActor.path().name()), probe.getRef());
                probe.expectMsgClass(InitiateHandoverRequest.class);
            }
        };
    }

    @Test
    public void testProxyForwardsStart() {
        new TestKit(system) {
            {
                proxyActor.tell(new StartHandoverRequest(proxyActor.path().name()), probe.getRef());
                probe.expectMsgClass(StartHandoverRequest.class);
            }
        };
    }

    @Test
    public void testProxyRespondsToInit() {
        new TestKit(system) {
            {
                proxyActor.tell(new InitiateHandoverRequest(proxyActor.path().name()), probe.getRef());
                probe.expectMsgClass(InitiateHandoverRequest.class);
                //Emulate serverHandshakeFU response
                responseConnector.publish(new ServerHandshakeResponseEvent(probeId,
                        HandshakeCapability.ServerMessageTypes.OkResponseInitHandover));
                probe.expectMsgClass(ServerHandshakeResponseEvent.class);
            }
        };
    }

    @Test
    public void testProxyRespondsToStart(){
        new TestKit(system) {
            {
                proxyActor.tell(new StartHandoverRequest(proxyActor.path().name()), probe.getRef());
                probe.expectMsgClass(StartHandoverRequest.class);
                //Emulate serverHandshakeFU response
                responseConnector.publish(new ServerHandshakeResponseEvent(probeId,
                        HandshakeCapability.ServerMessageTypes.OkResponseStartHandover));
                probe.expectMsgClass(ServerHandshakeResponseEvent.class);
            }
        };
    }
}
