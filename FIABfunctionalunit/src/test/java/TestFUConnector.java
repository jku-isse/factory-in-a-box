import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.functionalunit.ResetRequest;
import fiab.functionalunit.connector.FUConnector;
import fiab.functionalunit.connector.FUSubscriptionClassifier;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestFUConnector {

    private static ActorSystem system;
    private static TestKit probe;
    private static TestKit sender;
    private static String probeEventSourceId;
    private static String senderEventSourceId;
    private static FUSubscriptionClassifier classifier;

    private FUConnector connector;

    @BeforeAll
    static void setup(){
        system = ActorSystem.create();
        probe = new TestKit(system);
        sender = new TestKit(system);
        probeEventSourceId = probe.getRef().path().name();
        senderEventSourceId = sender.getRef().path().name();
        classifier = new FUSubscriptionClassifier(probeEventSourceId, "*");
    }

    @BeforeEach
    void init(){
        connector = new FUConnector();
        connector.subscribe(probe.getRef(), classifier);
        connector.subscribe(sender.getRef(), new FUSubscriptionClassifier(senderEventSourceId, "*"));
    }

    @AfterEach
    void tearDown(){
        connector = null;
    }

    @AfterAll
    static void cleanup(){
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    public void testNoSenderOnPublishThrowsException(){
        assertThrows(NullPointerException.class, () ->
                connector.publish(new ResetRequest("test"), ActorRef.noSender()));
    }

    @Test
    public void testFUMessageCanBeReceivedBySubscriber(){
        //Don't use publish(req, actorRef), since this won't match the message to subscribers -> no message from sender
        connector.publish(new ResetRequest(senderEventSourceId));
        probe.expectMsgClass(ResetRequest.class);
    }

    @Test
    public void testFUMessageIsNotSentToSender(){
        //Don't use publish(req, actorRef), since this won't match the message to subscribers, -> message received
        connector.publish(new ResetRequest(probeEventSourceId));
        probe.expectNoMessage();
    }
}
