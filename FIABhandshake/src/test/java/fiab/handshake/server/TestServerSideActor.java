package fiab.handshake.server;

import akka.actor.ActorRef;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.functionalunit.ResetRequest;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.core.capabilities.handshake.client.CompleteHandshake;
import fiab.core.capabilities.handshake.server.InitiateHandoverRequest;
import fiab.core.capabilities.handshake.server.StartHandoverRequest;
import fiab.core.capabilities.handshake.server.TransportAreaStatusOverrideRequest;
import fiab.functionalunit.connector.FUConnector;
import fiab.handshake.connector.ServerNotificationConnector;
import fiab.handshake.connector.ServerResponseConnector;
import fiab.handshake.server.messages.ServerHandshakeResponseEvent;
import fiab.handshake.server.messages.ServerHandshakeStatusUpdateEvent;
import org.junit.jupiter.api.*;
import testutils.ActorTestInfrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("UnitTest")
public class TestServerSideActor {

    private static ActorTestInfrastructure infrastructure;
    private FUConnector fuConnector;
    private ServerResponseConnector responseConnector;
    private ServerNotificationConnector notificationConnector;

    @BeforeAll
    public static void init() {
        infrastructure = new ActorTestInfrastructure();
    }

    @BeforeEach
    public void setup() {
        fuConnector = new FUConnector();
        responseConnector = new ServerResponseConnector();
        infrastructure.subscribeToIntraMachineEventBus();
        responseConnector.subscribe(probe().getRef(), infrastructure.getTestClassifier());
        infrastructure.initializeActor(ServerSideHandshakeActor
                        .props(fuConnector, infrastructure.getIntraMachineEventBus(), responseConnector, notificationConnector),
                "ServerSideHandshakeActor" + infrastructure.getAndIncrementRunCount());
    }

    @AfterEach
    public void teardown() {
        infrastructure.getIntraMachineEventBus().unsubscribe(probe().getRef());
        responseConnector.unsubscribe(probe().getRef());
        infrastructure.destroyActor();
    }

    @AfterAll
    public static void cleanup() {
        infrastructure.shutdownInfrastructure();
    }

    @Test
    public void testFullHandshakeEmptySuccess() {
        expectServerSideState(ServerSideStates.STOPPED);
        actor().tell(new ResetRequest(infrastructure.eventSourceId), ActorRef.noSender());
        expectServerSideState(ServerSideStates.RESETTING);
        expectServerSideState(ServerSideStates.IDLE_EMPTY);

        actor().tell(new InitiateHandoverRequest(infrastructure.eventSourceId), ActorRef.noSender());
        expectServerSideState(ServerSideStates.STARTING);

        ServerHandshakeResponseEvent response = probe().expectMsgClass(ServerHandshakeResponseEvent.class);
        assertEquals(HandshakeCapability.ServerMessageTypes.OkResponseInitHandover, response.getResponse());

        expectServerSideState(ServerSideStates.PREPARING);
        expectServerSideState(ServerSideStates.READY_EMPTY);

        actor().tell(new StartHandoverRequest(infrastructure.eventSourceId), ActorRef.noSender());
        expectServerSideState(ServerSideStates.EXECUTE);

        response = probe().expectMsgClass(ServerHandshakeResponseEvent.class);
        assertEquals(HandshakeCapability.ServerMessageTypes.OkResponseStartHandover, response.getResponse());

        actor().tell(new CompleteHandshake(infrastructure.eventSourceId), ActorRef.noSender());
        expectServerSideState(ServerSideStates.COMPLETING);
        expectServerSideState(ServerSideStates.COMPLETE);
    }

    @Test
    public void testFullHandshakeLoadedSuccess() {
        actor().tell(new TransportAreaStatusOverrideRequest(infrastructure.eventSourceId,
                HandshakeCapability.StateOverrideRequests.SetLoaded), ActorRef.noSender());

        expectServerSideState(ServerSideStates.STOPPED);
        actor().tell(new ResetRequest(infrastructure.eventSourceId), ActorRef.noSender());
        expectServerSideState(ServerSideStates.RESETTING);
        expectServerSideState(ServerSideStates.IDLE_LOADED);

        actor().tell(new InitiateHandoverRequest(infrastructure.eventSourceId), ActorRef.noSender());
        expectServerSideState(ServerSideStates.STARTING);

        ServerHandshakeResponseEvent response = probe().expectMsgClass(ServerHandshakeResponseEvent.class);
        assertEquals(HandshakeCapability.ServerMessageTypes.OkResponseInitHandover, response.getResponse());

        expectServerSideState(ServerSideStates.PREPARING);
        expectServerSideState(ServerSideStates.READY_LOADED);

        actor().tell(new StartHandoverRequest(infrastructure.eventSourceId), ActorRef.noSender());
        expectServerSideState(ServerSideStates.EXECUTE);

        response = probe().expectMsgClass(ServerHandshakeResponseEvent.class);
        assertEquals(HandshakeCapability.ServerMessageTypes.OkResponseStartHandover, response.getResponse());

        actor().tell(new CompleteHandshake(infrastructure.eventSourceId), ActorRef.noSender());
        expectServerSideState(ServerSideStates.COMPLETING);
        expectServerSideState(ServerSideStates.COMPLETE);
    }

    @Test
    public void testHandshakeEmptyThenLoadedFails() {
        expectServerSideState(ServerSideStates.STOPPED);
        actor().tell(new ResetRequest(infrastructure.eventSourceId), ActorRef.noSender());
        expectServerSideState(ServerSideStates.RESETTING);
        expectServerSideState(ServerSideStates.IDLE_EMPTY);

        actor().tell(new TransportAreaStatusOverrideRequest(infrastructure.eventSourceId,
                HandshakeCapability.StateOverrideRequests.SetLoaded), ActorRef.noSender());
        actor().tell(new InitiateHandoverRequest(infrastructure.eventSourceId), ActorRef.noSender());
        expectServerSideState(ServerSideStates.STARTING);

        ServerHandshakeResponseEvent response = probe().expectMsgClass(ServerHandshakeResponseEvent.class);
        assertEquals(HandshakeCapability.ServerMessageTypes.OkResponseInitHandover, response.getResponse());

        expectServerSideState(ServerSideStates.PREPARING);
        expectServerSideState(ServerSideStates.STOPPING);
        expectServerSideState(ServerSideStates.STOPPED);
    }

    @Test
    public void testHandshakeLoadedThenEmptyFails() {
        expectServerSideState(ServerSideStates.STOPPED);
        actor().tell(new TransportAreaStatusOverrideRequest(infrastructure.eventSourceId,
                HandshakeCapability.StateOverrideRequests.SetLoaded), ActorRef.noSender());
        actor().tell(new ResetRequest(infrastructure.eventSourceId), ActorRef.noSender());
        expectServerSideState(ServerSideStates.RESETTING);
        expectServerSideState(ServerSideStates.IDLE_LOADED);

        actor().tell(new TransportAreaStatusOverrideRequest(infrastructure.eventSourceId,
                HandshakeCapability.StateOverrideRequests.SetEmpty), ActorRef.noSender());
        actor().tell(new InitiateHandoverRequest(infrastructure.eventSourceId), ActorRef.noSender());
        expectServerSideState(ServerSideStates.STARTING);

        ServerHandshakeResponseEvent response = probe().expectMsgClass(ServerHandshakeResponseEvent.class);
        assertEquals(HandshakeCapability.ServerMessageTypes.OkResponseInitHandover, response.getResponse());

        expectServerSideState(ServerSideStates.PREPARING);
        expectServerSideState(ServerSideStates.STOPPING);
        expectServerSideState(ServerSideStates.STOPPED);
    }

    @Test
    public void testServerRespondsWithNotOkInitInWrongState() {
        expectServerSideState(ServerSideStates.STOPPED);
        actor().tell(new InitiateHandoverRequest(infrastructure.eventSourceId), ActorRef.noSender());
        ServerHandshakeResponseEvent response = probe().expectMsgClass(ServerHandshakeResponseEvent.class);
        assertEquals(HandshakeCapability.ServerMessageTypes.NotOkResponseInitHandover, response.getResponse());
    }

    @Test
    public void testServerRespondsWithNotOkStartInWrongState() {
        expectServerSideState(ServerSideStates.STOPPED);
        actor().tell(new StartHandoverRequest(infrastructure.eventSourceId), ActorRef.noSender());
        ServerHandshakeResponseEvent response = probe().expectMsgClass(ServerHandshakeResponseEvent.class);
        assertEquals(HandshakeCapability.ServerMessageTypes.NotOkResponseStartHandover, response.getResponse());
    }

    @Test
    public void testServerRespondsWithNotOkStartWhenLoadedAfterIdleReset() {
        expectServerSideState(ServerSideStates.STOPPED);
        actor().tell(new ResetRequest(infrastructure.eventSourceId), ActorRef.noSender());
        expectServerSideState(ServerSideStates.RESETTING);
        expectServerSideState(ServerSideStates.IDLE_EMPTY);

        actor().tell(new InitiateHandoverRequest(infrastructure.eventSourceId), ActorRef.noSender());
        expectServerSideState(ServerSideStates.STARTING);

        ServerHandshakeResponseEvent response = probe().expectMsgClass(ServerHandshakeResponseEvent.class);
        assertEquals(HandshakeCapability.ServerMessageTypes.OkResponseInitHandover, response.getResponse());

        expectServerSideState(ServerSideStates.PREPARING);
        expectServerSideState(ServerSideStates.READY_EMPTY);

        actor().tell(new TransportAreaStatusOverrideRequest(infrastructure.eventSourceId,
                HandshakeCapability.StateOverrideRequests.SetLoaded), ActorRef.noSender());
        actor().tell(new StartHandoverRequest(infrastructure.eventSourceId), ActorRef.noSender());

        expectServerSideState(ServerSideStates.STOPPING);
        expectServerSideState(ServerSideStates.STOPPED);

        response = probe().expectMsgClass(ServerHandshakeResponseEvent.class);
        assertEquals(HandshakeCapability.ServerMessageTypes.NotOkResponseStartHandover, response.getResponse());
    }

    @Test
    public void testServerRespondsWithNotOkStartWhenLEmptyAfterLoadedReset() {
        expectServerSideState(ServerSideStates.STOPPED);
        actor().tell(new TransportAreaStatusOverrideRequest(infrastructure.eventSourceId,
                HandshakeCapability.StateOverrideRequests.SetLoaded), ActorRef.noSender());
        actor().tell(new ResetRequest(infrastructure.eventSourceId), ActorRef.noSender());
        expectServerSideState(ServerSideStates.RESETTING);
        expectServerSideState(ServerSideStates.IDLE_LOADED);

        actor().tell(new InitiateHandoverRequest(infrastructure.eventSourceId), ActorRef.noSender());
        expectServerSideState(ServerSideStates.STARTING);

        ServerHandshakeResponseEvent response = probe().expectMsgClass(ServerHandshakeResponseEvent.class);
        assertEquals(HandshakeCapability.ServerMessageTypes.OkResponseInitHandover, response.getResponse());

        expectServerSideState(ServerSideStates.PREPARING);
        expectServerSideState(ServerSideStates.READY_LOADED);

        actor().tell(new TransportAreaStatusOverrideRequest(infrastructure.eventSourceId,
                HandshakeCapability.StateOverrideRequests.SetEmpty), ActorRef.noSender());
        actor().tell(new StartHandoverRequest(infrastructure.eventSourceId), ActorRef.noSender());

        expectServerSideState(ServerSideStates.STOPPING);
        expectServerSideState(ServerSideStates.STOPPED);

        response = probe().expectMsgClass(ServerHandshakeResponseEvent.class);
        assertEquals(HandshakeCapability.ServerMessageTypes.NotOkResponseStartHandover, response.getResponse());
    }


    private void expectServerSideState(ServerSideStates state) {
        ServerHandshakeStatusUpdateEvent statusUpdate = probe().expectMsgClass(ServerHandshakeStatusUpdateEvent.class);
        assertEquals(state, statusUpdate.getStatus());
    }

    private ActorRef actor() {
        return infrastructure.getActorRef();
    }

    private TestKit probe() {
        return infrastructure.getProbe();
    }
}
