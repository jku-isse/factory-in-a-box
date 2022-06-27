package fiab.handshake.client;

import akka.actor.ActorRef;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.functionalunit.ResetRequest;
import fiab.core.capabilities.functionalunit.StopRequest;
import fiab.core.capabilities.handshake.ClientSideStates;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.core.capabilities.handshake.client.CompleteHandshake;
import fiab.core.capabilities.handshake.client.PerformHandshake;
import fiab.core.capabilities.handshake.server.InitiateHandoverRequest;
import fiab.core.capabilities.handshake.server.StartHandoverRequest;
import fiab.core.capabilities.handshake.server.SubscribeToUpdatesRequest;
import fiab.core.capabilities.handshake.server.UnsubscribeToUpdatesRequest;
import fiab.functionalunit.connector.FUConnector;
import fiab.handshake.client.messages.ClientHandshakeStatusUpdateEvent;
import fiab.handshake.connector.ServerNotificationConnector;
import fiab.handshake.connector.ServerResponseConnector;
import fiab.handshake.server.messages.ServerHandshakeResponseEvent;
import fiab.handshake.server.messages.ServerHandshakeStatusUpdateEvent;
import org.junit.jupiter.api.*;
import testutils.ActorTestInfrastructure;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("UnitTest")
public class TestClientSideActor {

    private static ActorTestInfrastructure infrastructure;
    private static FUConnector clientConnector;                         //Send messages to client hs

    private static FUConnector remoteFUConnector;                       //Client sends requests here
    private static ServerResponseConnector responseConnector;           //Server responses go here
    private static ServerNotificationConnector notificationConnector;   //Server status updates go here

    @BeforeAll
    static void setup() {
        infrastructure = new ActorTestInfrastructure();
        infrastructure.subscribeToIntraMachineEventBus();
    }

    @BeforeEach
    void init() {
        clientConnector = new FUConnector();
        remoteFUConnector = new FUConnector();
        responseConnector = new ServerResponseConnector();
        notificationConnector = new ServerNotificationConnector();

        remoteFUConnector.subscribe(infrastructure.getProbe().getRef(), infrastructure.getTestClassifier());
        responseConnector.subscribe(infrastructure.getProbe().getRef(), infrastructure.getTestClassifier());
        notificationConnector.subscribe(infrastructure.getProbe().getRef(), infrastructure.getTestClassifier());

        infrastructure.initializeActor(ClientSideHandshakeActor.propsLocalHandshake(clientConnector, infrastructure.getIntraMachineEventBus(),
                        remoteFUConnector, responseConnector, notificationConnector),
                "ClientSideHandshakeTestActor" + infrastructure.getAndIncrementRunCount());
        expectClientHandshakeState(ClientSideStates.STOPPED);
    }

    @AfterEach
    void teardown() {
        infrastructure.destroyActor();
        remoteFUConnector.unsubscribe(infrastructure.getProbe().getRef());
        responseConnector.unsubscribe(infrastructure.getProbe().getRef());
        notificationConnector.unsubscribe(infrastructure.getProbe().getRef());
    }

    @AfterAll
    static void cleanup() {
        infrastructure.shutdownInfrastructure();
    }

    @Test
    public void testCorrectHandshakeBehaviorEmpty() {
        new TestKit(infrastructure.getSystem()) {
            {
                performFullHandshakeCycle(true);
            }
        };
    }

    @Test
    public void testCorrectHandshakeBehaviorFull() {
        new TestKit(infrastructure.getSystem()) {
            {
                performFullHandshakeCycle(false);
            }
        };
    }

    @Test
    public void testCorrectHandshakeBehavior5CyclesEmpty() {
        new TestKit(infrastructure.getSystem()) {
            {
                for(int i = 0; i < 5; i++) {
                    performFullHandshakeCycle(true);
                }
            }
        };
    }

    @Test
    public void testCorrectHandshakeBehavior5CyclesFull() {
        new TestKit(infrastructure.getSystem()) {
            {
                for(int i = 0; i < 5; i++) {
                    performFullHandshakeCycle(false);
                }
            }
        };
    }

    @Test
    public void testCorrectHandshakeBehavior5CyclesEmptyFull() {
        new TestKit(infrastructure.getSystem()) {
            {
                for(int i = 0; i < 5; i++) {
                    performFullHandshakeCycle(i % 2 == 0);
                }
            }
        };
    }

    @Test
    public void testHandshakeStopping() {
        actorRef().tell(new ResetRequest(infrastructure.eventSourceId), ActorRef.noSender());
        expectClientHandshakeState(ClientSideStates.RESETTING);
        expectClientHandshakeState(ClientSideStates.IDLE);
        actorRef().tell(new StopRequest(infrastructure.eventSourceId), ActorRef.noSender());
        expectClientHandshakeState(ClientSideStates.STOPPING);
        probe().expectMsgClass(UnsubscribeToUpdatesRequest.class);
        expectClientHandshakeState(ClientSideStates.STOPPED);
    }

    @Test
    public void testHandshakeWithTimeoutOnInitRetry() {
        performHandshakeUpToInitiating(true);
        probe().expectMsgClass(Duration.ofSeconds(6), InitiateHandoverRequest.class);
        probe().expectMsgClass(Duration.ofSeconds(6), InitiateHandoverRequest.class);
    }

    @Test
    public void testHandshakeContinuesAfterInitRetry(){
        performHandshakeUpToInitiating(true);
        probe().expectMsgClass(Duration.ofSeconds(6), InitiateHandoverRequest.class);
        resumeHandshakeUpToReady(true);
        resumeToCompleteHandshake(true);
    }

    @Test
    public void testHandshakeWithTimeoutOnStart() {
        performHandshakeUpToInitiating(true);
        resumeHandshakeUpToReady(true);
        probe().expectMsgClass(Duration.ofSeconds(6), StartHandoverRequest.class);
        probe().expectMsgClass(Duration.ofSeconds(6), StartHandoverRequest.class);
    }

    @Test
    public void testHandshakeContinuesAfterStartRetry() {
        performHandshakeUpToInitiating(true);
        resumeHandshakeUpToReady(true);
        probe().expectMsgClass(Duration.ofSeconds(6), StartHandoverRequest.class);
        resumeToCompleteHandshake(true);
    }

    @Test
    public void testHandshakeRemoteServerStoppingDuringExecute() {
        performHandshakeUpToInitiating(true);
        resumeHandshakeUpToReady(true);

        notificationConnector.publish(new ServerHandshakeStatusUpdateEvent(infrastructure.eventSourceId, ServerSideStates.READY_EMPTY));
        responseConnector.publish(new ServerHandshakeResponseEvent(infrastructure.eventSourceId, HandshakeCapability.ServerMessageTypes.OkResponseStartHandover));
        expectClientHandshakeState(ClientSideStates.EXECUTE);

        notificationConnector.publish(new ServerHandshakeStatusUpdateEvent(infrastructure.eventSourceId, ServerSideStates.STOPPING));
        expectClientHandshakeState(ClientSideStates.STOPPING);
        probe().expectMsgClass(UnsubscribeToUpdatesRequest.class);
        expectClientHandshakeState(ClientSideStates.STOPPED);
    }

    private void performFullHandshakeCycle(boolean remoteEmpty){
        performHandshakeUpToInitiating(remoteEmpty);
        resumeHandshakeUpToReady(remoteEmpty);
        resumeToCompleteHandshake(remoteEmpty);
    }

    /**
     * Performs the handshake up to the point where the remote init request was sent
     */
    private void performHandshakeUpToInitiating(boolean remoteEmpty){
        /*For some reason this does not work??? Receive ends with ServerStatusUpdate???
         *clientConnector.publish(new ResetRequest(infrastructure.eventSourceId));*/
        actorRef().tell(new ResetRequest(infrastructure.eventSourceId), ActorRef.noSender());
        expectClientHandshakeState(ClientSideStates.RESETTING);
        expectClientHandshakeState(ClientSideStates.IDLE);

        actorRef().tell(new PerformHandshake(infrastructure.eventSourceId), ActorRef.noSender());
        expectClientHandshakeState(ClientSideStates.STARTING);
        probe().expectMsgClass(SubscribeToUpdatesRequest.class);
        if(remoteEmpty) {
            notificationConnector.publish(new ServerHandshakeStatusUpdateEvent(infrastructure.eventSourceId, ServerSideStates.IDLE_EMPTY));
        }else{
            notificationConnector.publish(new ServerHandshakeStatusUpdateEvent(infrastructure.eventSourceId, ServerSideStates.IDLE_LOADED));
        }
        expectClientHandshakeState(ClientSideStates.INITIATING);
        probe().expectMsgClass(InitiateHandoverRequest.class);
    }

    /**
     * Continues the handshake up to ready, where the start event was sent
     * Assuming the state is initiating and the init request was already sent
     */
    private void resumeHandshakeUpToReady(boolean remoteEmpty){
        responseConnector.publish(new ServerHandshakeResponseEvent(infrastructure.eventSourceId, HandshakeCapability.ServerMessageTypes.OkResponseInitHandover));
        notificationConnector.publish(new ServerHandshakeStatusUpdateEvent(infrastructure.eventSourceId, ServerSideStates.STARTING));
        expectClientHandshakeState(ClientSideStates.INITIATED);
        if(remoteEmpty){
            notificationConnector.publish(new ServerHandshakeStatusUpdateEvent(infrastructure.eventSourceId, ServerSideStates.READY_EMPTY));
        }else{
            notificationConnector.publish(new ServerHandshakeStatusUpdateEvent(infrastructure.eventSourceId, ServerSideStates.READY_LOADED));
        }
        expectClientHandshakeState(ClientSideStates.READY);
        probe().expectMsgClass(StartHandoverRequest.class);
    }

    /**
     * Assumes the state is Ready and the start request was already sent
     */
    private void resumeToCompleteHandshake(boolean remoteEmpty){
        responseConnector.publish(new ServerHandshakeResponseEvent(infrastructure.eventSourceId, HandshakeCapability.ServerMessageTypes.OkResponseStartHandover));

        expectClientHandshakeState(ClientSideStates.EXECUTE);

        actorRef().tell(new CompleteHandshake(infrastructure.eventSourceId), ActorRef.noSender());

        expectClientHandshakeState(ClientSideStates.COMPLETING);
        probe().expectMsgClass(UnsubscribeToUpdatesRequest.class);
        expectClientHandshakeState(ClientSideStates.COMPLETED);
    }

    private void expectClientHandshakeState(ClientSideStates state) {
        ClientHandshakeStatusUpdateEvent handshakeStatusUpdateEvent;
        handshakeStatusUpdateEvent = probe().expectMsgClass(ClientHandshakeStatusUpdateEvent.class);
        assertEquals(state, handshakeStatusUpdateEvent.getStatus());
    }

    private ActorRef actorRef() {
        return infrastructure.getActorRef();
    }

    private TestKit probe() {
        return infrastructure.getProbe();
    }
}
