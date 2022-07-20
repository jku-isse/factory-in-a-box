package fiab.handshake.fullHandshake;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.functionalunit.ResetRequest;
import fiab.core.capabilities.handshake.ClientSideStates;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.core.capabilities.handshake.client.CompleteHandshake;
import fiab.core.capabilities.handshake.client.PerformHandshake;
import fiab.core.capabilities.handshake.server.TransportAreaStatusOverrideRequest;
import fiab.functionalunit.connector.FUConnector;
import fiab.functionalunit.connector.FUSubscriptionClassifier;
import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.handshake.client.ClientSideHandshakeActor;
import fiab.handshake.client.messages.ClientHandshakeStatusUpdateEvent;
import fiab.handshake.connector.ServerNotificationConnector;
import fiab.handshake.connector.ServerResponseConnector;
import fiab.handshake.server.ServerSideHandshakeActor;
import fiab.handshake.server.messages.ServerHandshakeStatusUpdateEvent;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("IntegrationTest")
public class TestHandshakeProtocol {

    private static ActorSystem system;

    private IntraMachineEventBus serverIntraMachineBus;
    private FUConnector serverRequestBus;
    private ServerResponseConnector responseConnector;
    private ServerNotificationConnector notificationConnector;

    private IntraMachineEventBus clientIntraMachineBus;
    private FUConnector clientRequestBus;

    private String serverProbeId;
    private TestKit serverProbe;

    private String clientProbeId;
    private TestKit clientProbe;

    private ActorRef serverActor;
    private ActorRef clientActor;

    @BeforeAll
    public static void init() {
        system = ActorSystem.create();
    }

    @BeforeEach
    public void setup() {
        serverIntraMachineBus = new IntraMachineEventBus();
        serverRequestBus = new FUConnector();
        responseConnector = new ServerResponseConnector();
        notificationConnector = new ServerNotificationConnector();

        clientIntraMachineBus = new IntraMachineEventBus();
        clientRequestBus = new FUConnector();
        setupServerClientStructure();
    }

    @AfterEach
    public void teardown(){
        clientActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
        serverActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
        serverIntraMachineBus.unsubscribe(serverProbe.getRef());
        clientIntraMachineBus.unsubscribe(clientProbe.getRef());
    }

    @AfterAll
    public static void cleanup(){
        system.terminate();
    }

    private void setupServerClientStructure() {
        setupServerStructure();
        setupClientStructure();
    }

    private void setupServerStructure() {
        serverProbe = new TestKit(system);
        serverProbeId = serverProbe.getRef().path().name();
        serverIntraMachineBus.subscribe(serverProbe.getRef(), new FUSubscriptionClassifier(serverProbeId, "*"));
        serverActor = system.actorOf(ServerSideHandshakeActor.props(serverRequestBus, serverIntraMachineBus,
                responseConnector, notificationConnector), "ServerSide");
        expectServerSideState(ServerSideStates.STOPPED);
    }

    private void setupClientStructure() {
        clientProbe = new TestKit(system);
        clientProbeId = clientProbe.getRef().path().name();
        clientIntraMachineBus.subscribe(clientProbe.getRef(), new FUSubscriptionClassifier(clientProbeId, "*"));
        //use local here, since it does not wait for a nonexistent client to spawn, thus never leaving resetting
        clientActor = system.actorOf(ClientSideHandshakeActor.propsLocalHandshake(clientRequestBus, clientIntraMachineBus,
                serverRequestBus, responseConnector, notificationConnector), "ClientSide");
        expectClientSideState(ClientSideStates.STOPPED);
    }

    @Test
    public void testFullHandshakeCycleEmptyClientCompletesFirst(){
        new TestKit(system){
            {
                resetHandshakesWithEmptyServer();
                proceedToExecuteEmpty();

                clientRequestBus.publish(new CompleteHandshake(clientProbeId));
                expectClientSideState(ClientSideStates.COMPLETING);
                expectClientSideState(ClientSideStates.COMPLETED);

                serverRequestBus.publish(new CompleteHandshake(serverProbeId));
                expectServerSideState(ServerSideStates.COMPLETING);
                expectServerSideState(ServerSideStates.COMPLETE);
            }
        };
    }

    @Test
    public void testFullHandshakeCycleEmptyServerCompletesFirst(){
        new TestKit(system){
            {
                resetHandshakesWithEmptyServer();
                proceedToExecuteEmpty();

                serverRequestBus.publish(new CompleteHandshake(serverProbeId));
                expectServerSideState(ServerSideStates.COMPLETING);
                expectServerSideState(ServerSideStates.COMPLETE);

                clientRequestBus.publish(new CompleteHandshake(clientProbeId));
                expectClientSideState(ClientSideStates.COMPLETING);
                expectClientSideState(ClientSideStates.COMPLETED);
            }
        };
    }

    @Test
    public void testFullHandshakeCycleLoadedClientCompletesFirst(){
        new TestKit(system){
            {
                resetHandshakesWithLoadedServer();
                proceedToExecuteLoaded();

                clientRequestBus.publish(new CompleteHandshake(clientProbeId));
                expectClientSideState(ClientSideStates.COMPLETING);
                expectClientSideState(ClientSideStates.COMPLETED);

                serverRequestBus.publish(new CompleteHandshake(serverProbeId));
                expectServerSideState(ServerSideStates.COMPLETING);
                expectServerSideState(ServerSideStates.COMPLETE);
            }
        };
    }

    @Test
    public void testFullHandshakeCycleLoadedServerCompletesFirst(){
        new TestKit(system){
            {
                resetHandshakesWithLoadedServer();
                proceedToExecuteLoaded();

                serverRequestBus.publish(new CompleteHandshake(serverProbeId));
                expectServerSideState(ServerSideStates.COMPLETING);
                expectServerSideState(ServerSideStates.COMPLETE);

                clientRequestBus.publish(new CompleteHandshake(clientProbeId));
                expectClientSideState(ClientSideStates.COMPLETING);
                expectClientSideState(ClientSideStates.COMPLETED);
            }
        };
    }


    @Test
    public void testFullHandshakeCycleEmptyServerCompletesBeforeClientExecutes(){
        new TestKit(system){
            {
                resetHandshakesWithEmptyServer();
                clientRequestBus.publish(new PerformHandshake(clientProbeId));
                expectClientSideState(ClientSideStates.STARTING);

                expectServerSideState(ServerSideStates.IDLE_EMPTY);     //duplicate idle event, since we resend the event in case of local actors. Not necessary for opcua

                expectClientSideState(ClientSideStates.INITIATING);

                expectServerSideState(ServerSideStates.STARTING);
                expectServerSideState(ServerSideStates.PREPARING);
                expectServerSideState(ServerSideStates.READY_EMPTY);

                expectClientSideState(ClientSideStates.INITIATED);
                expectClientSideState(ClientSideStates.READY);

                expectServerSideState(ServerSideStates.EXECUTE);
                serverRequestBus.publish(new CompleteHandshake(serverProbeId));
                expectServerSideState(ServerSideStates.COMPLETING);
                expectServerSideState(ServerSideStates.COMPLETE);

                expectClientSideState(ClientSideStates.EXECUTE);
                clientRequestBus.publish(new CompleteHandshake(clientProbeId));
                expectClientSideState(ClientSideStates.COMPLETING);
                expectClientSideState(ClientSideStates.COMPLETED);
            }
        };
    }

    @Test
    public void testFullHandshakeCycleLoadedServerCompletesBeforeClientExecutes(){
        new TestKit(system){
            {
                resetHandshakesWithLoadedServer();
                clientRequestBus.publish(new PerformHandshake(clientProbeId));
                expectClientSideState(ClientSideStates.STARTING);

                expectServerSideState(ServerSideStates.IDLE_LOADED);     //duplicate idle event, since we resend the event in case of local actors. Not necessary for opcua

                expectClientSideState(ClientSideStates.INITIATING);

                expectServerSideState(ServerSideStates.STARTING);
                expectServerSideState(ServerSideStates.PREPARING);
                expectServerSideState(ServerSideStates.READY_LOADED);

                expectClientSideState(ClientSideStates.INITIATED);
                expectClientSideState(ClientSideStates.READY);

                expectServerSideState(ServerSideStates.EXECUTE);
                serverRequestBus.publish(new CompleteHandshake(serverProbeId));
                expectServerSideState(ServerSideStates.COMPLETING);
                expectServerSideState(ServerSideStates.COMPLETE);

                expectClientSideState(ClientSideStates.EXECUTE);
                clientRequestBus.publish(new CompleteHandshake(clientProbeId));
                expectClientSideState(ClientSideStates.COMPLETING);
                expectClientSideState(ClientSideStates.COMPLETED);
            }
        };
    }

    @Test
    public void testEmptyHandshakeMultipleCycles() {
        new TestKit(system){
            {
                for (int i = 0; i < 3; i++) {
                    resetHandshakesWithEmptyServer();
                    proceedToExecuteEmpty();
                    serverRequestBus.publish(new CompleteHandshake(serverProbeId));
                    expectServerSideState(ServerSideStates.COMPLETING);
                    expectServerSideState(ServerSideStates.COMPLETE);

                    clientRequestBus.publish(new CompleteHandshake(clientProbeId));
                    expectClientSideState(ClientSideStates.COMPLETING);
                    expectClientSideState(ClientSideStates.COMPLETED);
                }
            }
        };
    }

    @Test
    public void testLoadedHandshakeMultipleCycles(){
        new TestKit(system){
            {
                for (int i = 0; i < 3; i++) {
                    resetHandshakesWithLoadedServer();
                    proceedToExecuteLoaded();
                    serverRequestBus.publish(new CompleteHandshake(serverProbeId));
                    expectServerSideState(ServerSideStates.COMPLETING);
                    expectServerSideState(ServerSideStates.COMPLETE);

                    clientRequestBus.publish(new CompleteHandshake(clientProbeId));
                    expectClientSideState(ClientSideStates.COMPLETING);
                    expectClientSideState(ClientSideStates.COMPLETED);
                }
            }
        };
    }

    @Test
    public void testMultipleServerSwitchingBetweenEmptyAndLoadedFullHandshakeCycles(){
        for (int i = 0; i < 5; i++) {
            if(i % 2 == 0){
                resetHandshakesWithEmptyServer();
                proceedToExecuteEmpty();
            }else{
                resetHandshakesWithLoadedServer();
                proceedToExecuteLoaded();
            }
            serverRequestBus.publish(new CompleteHandshake(serverProbeId));
            expectServerSideState(ServerSideStates.COMPLETING);
            expectServerSideState(ServerSideStates.COMPLETE);

            clientRequestBus.publish(new CompleteHandshake(clientProbeId));
            expectClientSideState(ClientSideStates.COMPLETING);
            expectClientSideState(ClientSideStates.COMPLETED);
        }
    }

    private void resetHandshakesWithEmptyServer(){
        serverRequestBus.publish(new TransportAreaStatusOverrideRequest(serverProbeId,
                HandshakeCapability.StateOverrideRequests.SetEmpty));
        serverRequestBus.publish(new ResetRequest(serverProbeId));
        expectServerSideState(ServerSideStates.RESETTING);
        expectServerSideState(ServerSideStates.IDLE_EMPTY);

        clientRequestBus.publish(new ResetRequest(clientProbeId));
        expectClientSideState(ClientSideStates.RESETTING);
        expectClientSideState(ClientSideStates.IDLE);
    }

    private void resetHandshakesWithLoadedServer(){
        serverRequestBus.publish(new TransportAreaStatusOverrideRequest(serverProbeId,
                HandshakeCapability.StateOverrideRequests.SetLoaded));
        serverRequestBus.publish(new ResetRequest(serverProbeId));
        expectServerSideState(ServerSideStates.RESETTING);
        expectServerSideState(ServerSideStates.IDLE_LOADED);

        clientRequestBus.publish(new ResetRequest(clientProbeId));
        expectClientSideState(ClientSideStates.RESETTING);
        expectClientSideState(ClientSideStates.IDLE);
    }

    private void proceedToExecuteEmpty(){
        clientRequestBus.publish(new PerformHandshake(clientProbeId));
        expectClientSideState(ClientSideStates.STARTING);

        expectServerSideState(ServerSideStates.IDLE_EMPTY);

        expectClientSideState(ClientSideStates.INITIATING);

        expectServerSideState(ServerSideStates.STARTING);
        expectServerSideState(ServerSideStates.PREPARING);
        expectServerSideState(ServerSideStates.READY_EMPTY);

        expectClientSideState(ClientSideStates.INITIATED);
        expectClientSideState(ClientSideStates.READY);

        expectServerSideState(ServerSideStates.EXECUTE);

        expectClientSideState(ClientSideStates.EXECUTE);
    }

    private void proceedToExecuteLoaded(){
        clientRequestBus.publish(new PerformHandshake(clientProbeId));
        expectClientSideState(ClientSideStates.STARTING);

        expectServerSideState(ServerSideStates.IDLE_LOADED);

        expectClientSideState(ClientSideStates.INITIATING);

        expectServerSideState(ServerSideStates.STARTING);
        expectServerSideState(ServerSideStates.PREPARING);
        expectServerSideState(ServerSideStates.READY_LOADED);

        expectClientSideState(ClientSideStates.INITIATED);
        expectClientSideState(ClientSideStates.READY);

        expectServerSideState(ServerSideStates.EXECUTE);

        expectClientSideState(ClientSideStates.EXECUTE);
    }

    private void expectServerSideState(ServerSideStates state){
        ServerHandshakeStatusUpdateEvent event;
        event = serverProbe.expectMsgClass(ServerHandshakeStatusUpdateEvent.class);
        assertEquals(state, event.getStatus());
    }

    private void expectClientSideState(ClientSideStates state){
        ClientHandshakeStatusUpdateEvent event;
        event = clientProbe.expectMsgClass(ClientHandshakeStatusUpdateEvent.class);
        assertEquals(state, event.getStatus());
    }
}
