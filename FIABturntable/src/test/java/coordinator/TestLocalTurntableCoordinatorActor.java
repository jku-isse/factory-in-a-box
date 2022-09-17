package coordinator;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import coordinator.infrastructure.MachineTestChildFUs;
import fiab.core.capabilities.functionalunit.ResetRequest;
import fiab.core.capabilities.functionalunit.StopRequest;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.core.capabilities.handshake.client.CompleteHandshake;
import fiab.core.capabilities.handshake.client.PerformHandshake;
import fiab.core.capabilities.handshake.server.InitiateHandoverRequest;
import fiab.core.capabilities.handshake.server.StartHandoverRequest;
import fiab.core.capabilities.handshake.server.SubscribeToUpdatesRequest;
import fiab.core.capabilities.handshake.server.UnsubscribeToUpdatesRequest;
import fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers;
import fiab.functionalunit.connector.FUConnector;
import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.handshake.client.ClientSideHandshakeActor;
import fiab.handshake.client.messages.ClientHandshakeStatusUpdateEvent;
import fiab.handshake.connector.ServerNotificationConnector;
import fiab.handshake.connector.ServerResponseConnector;
import fiab.handshake.server.ServerSideHandshakeActor;
import fiab.handshake.server.messages.ServerHandshakeResponseEvent;
import fiab.handshake.server.messages.ServerHandshakeStatusUpdateEvent;
import fiab.turntable.TurntableCoordinatorActor;
import fiab.conveyor.ConveyorActor;
import fiab.conveyor.messages.ConveyorStatusUpdateEvent;
import fiab.conveyor.messages.LoadConveyorRequest;
import fiab.conveyor.messages.UnloadConveyorRequest;
import fiab.turntable.turning.TurningActor;
import fiab.turntable.turning.messages.TurnRequest;
import fiab.turntable.turning.messages.TurningStatusUpdateEvent;
import testutils.ActorTestInfrastructure;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.transport.TransportModuleRequest;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static fiab.core.capabilities.handshake.HandshakeCapability.ServerMessageTypes.OkResponseInitHandover;
import static fiab.core.capabilities.handshake.HandshakeCapability.ServerMessageTypes.OkResponseStartHandover;
import static fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_NORTH_SERVER;
import static fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_SOUTH_CLIENT;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("UnitTest")
public class TestLocalTurntableCoordinatorActor {

    private static ActorTestInfrastructure infrastructure;
    private List<ActorRef> fuActors;

    private TestKit intraMachineProbe;
    //TurningFU relevant probes and connectors
    private TestKit turningProbe;
    private FUConnector turningConnector;
    //ConveyorFU relevant probes and connectors
    private TestKit conveyorProbe;
    private FUConnector conveyorConnector;
    //ServerHandshakeFU relevant probes and connectors
    private TestKit serverHsProbe;
    private FUConnector serverHsConnector;
    private ServerNotificationConnector serverHsNotificationConnector;
    private ServerResponseConnector serverHsResponseConnector;
    //ClientHandshakeFU relevant probes and connectors
    private TestKit clientHsProbe;
    private FUConnector clientHsConnector;
    private TestKit serverHsProxy;  //Used to receive and respond to remote events
    private FUConnector clientRemoteConnector;
    private ServerNotificationConnector clientHsNotificationConnector;
    private ServerResponseConnector clientHsResponseConnector;


    @BeforeAll
    public static void setup() {
        infrastructure = new ActorTestInfrastructure();
    }

    @BeforeEach
    public void init() {
        fuActors = new ArrayList<>();
        infrastructure.subscribeToMachineEventBus();
        MachineTestChildFUs fus = initializeTurntableInfraStructureWithAttachedProxies();

        Props coordinatorActor = TurntableCoordinatorActor.props(infrastructure.getMachineEventBus(),
                infrastructure.getIntraMachineEventBus(), fus);
        infrastructure.initializeActor(coordinatorActor, "TurntableCoordinatorActor");

        initializeFUsAndCheckInitialState();

        expectCoordinatorState(BasicMachineStates.STOPPED);
    }

    @AfterEach
    public void teardown() {
        infrastructure.getMachineEventBus().unsubscribe(getProbe().getRef());
        infrastructure.destroyActor();
        unsubscribeFromAllConnectors();
    }

    @AfterAll
    public static void cleanup() {
        infrastructure.shutdownInfrastructure();
    }

    @Test
    public void testCoordinatorResettingReachesIdle() {
        actorRef().tell(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.Reset, ActorRef.noSender());
        expectCoordinatorState(BasicMachineStates.RESETTING);
        turningProbe.expectMsgClass(ResetRequest.class);
        conveyorProbe.expectMsgClass(ResetRequest.class);
        expectCoordinatorState(BasicMachineStates.IDLE);
    }

    @Test
    public void testCoordinatorStopWhileResetting() {
        actorRef().tell(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.Reset, ActorRef.noSender());
        expectCoordinatorState(BasicMachineStates.RESETTING);
        turningProbe.expectMsgClass(ResetRequest.class);
        conveyorProbe.expectMsgClass(ResetRequest.class);
        actorRef().tell(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.Stop, ActorRef.noSender());
        expectCoordinatorState(BasicMachineStates.STOPPING);
        turningProbe.expectMsgClass(StopRequest.class);
        conveyorProbe.expectMsgClass(StopRequest.class);
        expectCoordinatorState(BasicMachineStates.STOPPED);
    }

    @Test
    public void testCoordinatorTransportRequestReachesExecute() {
        actorRef().tell(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.Reset, ActorRef.noSender());
        expectCoordinatorState(BasicMachineStates.RESETTING);
        turningProbe.expectMsgClass(ResetRequest.class);
        conveyorProbe.expectMsgClass(ResetRequest.class);
        expectCoordinatorState(BasicMachineStates.IDLE);
        actorRef().tell(new TransportModuleRequest(TRANSPORT_MODULE_NORTH_SERVER, TRANSPORT_MODULE_SOUTH_CLIENT,
                "order-1", "id-1"), ActorRef.noSender());
        expectCoordinatorState(BasicMachineStates.STARTING);
        expectCoordinatorState(BasicMachineStates.EXECUTE);
        turningProbe.expectMsgClass(TurnRequest.class);
    }

    @Test
    public void testCoordinatorFullServerToServerSuccessful() {
        actorRef().tell(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.Reset, ActorRef.noSender());
        expectCoordinatorState(BasicMachineStates.RESETTING);
        turningProbe.expectMsgClass(ResetRequest.class);
        conveyorProbe.expectMsgClass(ResetRequest.class);
        expectCoordinatorState(BasicMachineStates.IDLE);
        actorRef().tell(new TransportModuleRequest(TRANSPORT_MODULE_NORTH_SERVER, TRANSPORT_MODULE_NORTH_SERVER,
                "TestOrder", "req1"), ActorRef.noSender());
        expectCoordinatorState(BasicMachineStates.STARTING);
        expectCoordinatorState(BasicMachineStates.EXECUTE);

        turningProbe.expectMsgClass(TurnRequest.class);
        serverHsProbe.expectMsgClass(ResetRequest.class);
        //Mimic remote client handshake
        serverHsConnector.publish(new InitiateHandoverRequest(infrastructure.eventSourceId));
        serverHsConnector.publish(new StartHandoverRequest(infrastructure.eventSourceId));

        conveyorProbe.expectMsgAnyClassOf(Duration.ofSeconds(3), LoadConveyorRequest.class, UnloadConveyorRequest.class);
        serverHsProbe.expectMsgClass(CompleteHandshake.class);
        turningProbe.expectMsgClass(ResetRequest.class);
        serverHsProbe.expectMsgClass(ResetRequest.class);
        //Mimic remote client handshake
        serverHsConnector.publish(new InitiateHandoverRequest(infrastructure.eventSourceId));
        serverHsConnector.publish(new StartHandoverRequest(infrastructure.eventSourceId));

        conveyorProbe.expectMsgAnyClassOf(Duration.ofSeconds(3), LoadConveyorRequest.class, UnloadConveyorRequest.class);

        expectCoordinatorState(BasicMachineStates.COMPLETING);
        expectCoordinatorState(BasicMachineStates.COMPLETE);
    }

    @Test
    public void testCoordinatorFullServerToClientSuccessful() {
        actorRef().tell(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.Reset, ActorRef.noSender());
        expectCoordinatorState(BasicMachineStates.RESETTING);
        turningProbe.expectMsgClass(ResetRequest.class);
        conveyorProbe.expectMsgClass(ResetRequest.class);
        expectCoordinatorState(BasicMachineStates.IDLE);
        actorRef().tell(new TransportModuleRequest(TRANSPORT_MODULE_NORTH_SERVER, TRANSPORT_MODULE_SOUTH_CLIENT,
                "TestOrder", "req1"), ActorRef.noSender());
        expectCoordinatorState(BasicMachineStates.STARTING);
        expectCoordinatorState(BasicMachineStates.EXECUTE);

        turningProbe.expectMsgClass(TurnRequest.class);
        serverHsProbe.expectMsgClass(ResetRequest.class);
        //Mimic remote client handshake
        serverHsConnector.publish(new InitiateHandoverRequest(infrastructure.eventSourceId));
        serverHsConnector.publish(new StartHandoverRequest(infrastructure.eventSourceId));

        conveyorProbe.expectMsgAnyClassOf(Duration.ofSeconds(3), LoadConveyorRequest.class, UnloadConveyorRequest.class);

        turningProbe.expectMsgClass(ResetRequest.class);
        clientHsProbe.expectMsgClass(ResetRequest.class);
        clientHsProbe.expectMsgClass(PerformHandshake.class);

        serverHsProxy.expectMsgClass(SubscribeToUpdatesRequest.class);
        clientHsNotificationConnector.publish(new ServerHandshakeStatusUpdateEvent(infrastructure.eventSourceId, ServerSideStates.IDLE_LOADED));

        serverHsProxy.expectMsgClass(InitiateHandoverRequest.class);
        clientHsResponseConnector.publish(new ServerHandshakeResponseEvent(infrastructure.eventSourceId, OkResponseInitHandover));
        clientHsNotificationConnector.publish(new ServerHandshakeStatusUpdateEvent(infrastructure.eventSourceId, ServerSideStates.READY_LOADED));

        serverHsProxy.expectMsgClass(StartHandoverRequest.class);
        clientHsResponseConnector.publish(new ServerHandshakeResponseEvent(infrastructure.eventSourceId, OkResponseStartHandover));

        conveyorProbe.expectMsgAnyClassOf(Duration.ofSeconds(3), LoadConveyorRequest.class, UnloadConveyorRequest.class);

        expectCoordinatorState(BasicMachineStates.COMPLETING);
        expectCoordinatorState(BasicMachineStates.COMPLETE);
    }

    @Test
    public void testCoordinatorFullClientToServerSuccessful() {
        actorRef().tell(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.Reset, ActorRef.noSender());
        expectCoordinatorState(BasicMachineStates.RESETTING);
        turningProbe.expectMsgClass(ResetRequest.class);
        conveyorProbe.expectMsgClass(ResetRequest.class);
        expectCoordinatorState(BasicMachineStates.IDLE);
        actorRef().tell(new TransportModuleRequest(TRANSPORT_MODULE_SOUTH_CLIENT, TRANSPORT_MODULE_NORTH_SERVER,
                "TestOrder", "req1"), ActorRef.noSender());
        expectCoordinatorState(BasicMachineStates.STARTING);
        expectCoordinatorState(BasicMachineStates.EXECUTE);

        turningProbe.expectMsgClass(TurnRequest.class);

        clientHsProbe.expectMsgClass(ResetRequest.class);
        clientHsProbe.expectMsgClass(PerformHandshake.class);
        serverHsProxy.expectMsgClass(SubscribeToUpdatesRequest.class);
        clientHsNotificationConnector.publish(new ServerHandshakeStatusUpdateEvent(infrastructure.eventSourceId, ServerSideStates.IDLE_LOADED));

        serverHsProxy.expectMsgClass(InitiateHandoverRequest.class);
        clientHsResponseConnector.publish(new ServerHandshakeResponseEvent(infrastructure.eventSourceId, OkResponseInitHandover));
        clientHsNotificationConnector.publish(new ServerHandshakeStatusUpdateEvent(infrastructure.eventSourceId, ServerSideStates.READY_LOADED));

        serverHsProxy.expectMsgClass(StartHandoverRequest.class);
        clientHsResponseConnector.publish(new ServerHandshakeResponseEvent(infrastructure.eventSourceId, OkResponseStartHandover));

        conveyorProbe.expectMsgAnyClassOf(Duration.ofSeconds(3), LoadConveyorRequest.class, UnloadConveyorRequest.class);

        turningProbe.expectMsgClass(ResetRequest.class);

        serverHsProbe.expectMsgClass(ResetRequest.class);
        //Mimic remote client handshake
        serverHsConnector.publish(new InitiateHandoverRequest(infrastructure.eventSourceId));
        serverHsConnector.publish(new StartHandoverRequest(infrastructure.eventSourceId));

        conveyorProbe.expectMsgAnyClassOf(Duration.ofSeconds(3), LoadConveyorRequest.class, UnloadConveyorRequest.class);

        expectCoordinatorState(BasicMachineStates.COMPLETING);
        expectCoordinatorState(BasicMachineStates.COMPLETE);
    }

    @Test
    public void testCoordinatorFullClientToClientSuccessful() {
        actorRef().tell(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.Reset, ActorRef.noSender());
        expectCoordinatorState(BasicMachineStates.RESETTING);
        turningProbe.expectMsgClass(ResetRequest.class);
        conveyorProbe.expectMsgClass(ResetRequest.class);
        expectCoordinatorState(BasicMachineStates.IDLE);
        actorRef().tell(new TransportModuleRequest(TRANSPORT_MODULE_SOUTH_CLIENT, TRANSPORT_MODULE_SOUTH_CLIENT,
                "TestOrder", "req1"), ActorRef.noSender());
        expectCoordinatorState(BasicMachineStates.STARTING);
        expectCoordinatorState(BasicMachineStates.EXECUTE);

        turningProbe.expectMsgClass(TurnRequest.class);

        clientHsProbe.expectMsgClass(ResetRequest.class);
        clientHsProbe.expectMsgClass(PerformHandshake.class);
        serverHsProxy.expectMsgClass(SubscribeToUpdatesRequest.class);
        clientHsNotificationConnector.publish(new ServerHandshakeStatusUpdateEvent(infrastructure.eventSourceId, ServerSideStates.IDLE_LOADED));

        serverHsProxy.expectMsgClass(InitiateHandoverRequest.class);
        clientHsResponseConnector.publish(new ServerHandshakeResponseEvent(infrastructure.eventSourceId, OkResponseInitHandover));
        clientHsNotificationConnector.publish(new ServerHandshakeStatusUpdateEvent(infrastructure.eventSourceId, ServerSideStates.READY_LOADED));

        serverHsProxy.expectMsgClass(StartHandoverRequest.class);
        clientHsResponseConnector.publish(new ServerHandshakeResponseEvent(infrastructure.eventSourceId, OkResponseStartHandover));

        conveyorProbe.expectMsgAnyClassOf(Duration.ofSeconds(3), LoadConveyorRequest.class, UnloadConveyorRequest.class);

        clientHsProbe.expectMsgClass(CompleteHandshake.class);
        serverHsProxy.expectMsgClass(UnsubscribeToUpdatesRequest.class);

        turningProbe.expectMsgClass(ResetRequest.class);
        turningProbe.expectMsgClass(TurnRequest.class);

        clientHsProbe.expectMsgClass(ResetRequest.class);
        clientHsProbe.expectMsgClass(PerformHandshake.class);
        serverHsProxy.expectMsgClass(SubscribeToUpdatesRequest.class);
        clientHsNotificationConnector.publish(new ServerHandshakeStatusUpdateEvent(infrastructure.eventSourceId, ServerSideStates.IDLE_LOADED));

        serverHsProxy.expectMsgClass(InitiateHandoverRequest.class);
        clientHsResponseConnector.publish(new ServerHandshakeResponseEvent(infrastructure.eventSourceId, OkResponseInitHandover));
        clientHsNotificationConnector.publish(new ServerHandshakeStatusUpdateEvent(infrastructure.eventSourceId, ServerSideStates.READY_LOADED));

        serverHsProxy.expectMsgClass(StartHandoverRequest.class);
        clientHsResponseConnector.publish(new ServerHandshakeResponseEvent(infrastructure.eventSourceId, OkResponseStartHandover));

        conveyorProbe.expectMsgAnyClassOf(Duration.ofSeconds(3), LoadConveyorRequest.class, UnloadConveyorRequest.class);

        expectCoordinatorState(BasicMachineStates.COMPLETING);
        expectCoordinatorState(BasicMachineStates.COMPLETE);
    }

    @Test
    public void testCoordinatorResetAfterCompleteSuccessful() {
        executeFullTransportProcessServerThenClientHs();
        actorRef().tell(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.Reset, ActorRef.noSender());
        expectCoordinatorState(BasicMachineStates.RESETTING);
        expectCoordinatorState(BasicMachineStates.IDLE);
    }

    @Test
    public void testStopAfterComplete() {
        executeFullTransportProcessServerThenClientHs();
        actorRef().tell(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.Stop, ActorRef.noSender());
        expectCoordinatorState(BasicMachineStates.STOPPING);
        expectCoordinatorState(BasicMachineStates.STOPPED);
    }

    @Test
    public void testTurntableStopsOnInvalidFromHandshake() {
        actorRef().tell(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.Reset, ActorRef.noSender());
        expectCoordinatorState(BasicMachineStates.RESETTING);
        expectCoordinatorState(BasicMachineStates.IDLE);
        actorRef().tell(new TransportModuleRequest("InvalidCap", TRANSPORT_MODULE_SOUTH_CLIENT,
                "TestOrder", "req1"), ActorRef.noSender());
        expectCoordinatorState(BasicMachineStates.STARTING);
        expectCoordinatorState(BasicMachineStates.STOPPING);
        expectCoordinatorState(BasicMachineStates.STOPPED);
    }

    @Test
    public void testTurntableStopsOnInvalidToHandshake() {
        actorRef().tell(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.Reset, ActorRef.noSender());
        expectCoordinatorState(BasicMachineStates.RESETTING);
        expectCoordinatorState(BasicMachineStates.IDLE);
        actorRef().tell(new TransportModuleRequest(TRANSPORT_MODULE_SOUTH_CLIENT, "InvalidCap",
                "TestOrder", "req1"), ActorRef.noSender());
        expectCoordinatorState(BasicMachineStates.STARTING);
        expectCoordinatorState(BasicMachineStates.STOPPING);
        expectCoordinatorState(BasicMachineStates.STOPPED);
    }

    private void executeFullTransportProcessServerThenClientHs() {
        actorRef().tell(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.Reset, ActorRef.noSender());
        expectCoordinatorState(BasicMachineStates.RESETTING);
        turningProbe.expectMsgClass(ResetRequest.class);
        conveyorProbe.expectMsgClass(ResetRequest.class);
        expectCoordinatorState(BasicMachineStates.IDLE);
        actorRef().tell(new TransportModuleRequest(TRANSPORT_MODULE_NORTH_SERVER, TRANSPORT_MODULE_SOUTH_CLIENT,
                "TestOrder", "req1"), ActorRef.noSender());
        expectCoordinatorState(BasicMachineStates.STARTING);
        expectCoordinatorState(BasicMachineStates.EXECUTE);

        turningProbe.expectMsgClass(TurnRequest.class);
        serverHsProbe.expectMsgClass(ResetRequest.class);
        //Mimic remote client handshake
        serverHsConnector.publish(new InitiateHandoverRequest(infrastructure.eventSourceId));
        serverHsConnector.publish(new StartHandoverRequest(infrastructure.eventSourceId));

        conveyorProbe.expectMsgAnyClassOf(Duration.ofSeconds(3), LoadConveyorRequest.class, UnloadConveyorRequest.class);

        turningProbe.expectMsgClass(ResetRequest.class);
        clientHsProbe.expectMsgClass(ResetRequest.class);
        clientHsProbe.expectMsgClass(PerformHandshake.class);

        serverHsProxy.expectMsgClass(SubscribeToUpdatesRequest.class);
        clientHsNotificationConnector.publish(new ServerHandshakeStatusUpdateEvent(infrastructure.eventSourceId, ServerSideStates.IDLE_LOADED));

        serverHsProxy.expectMsgClass(InitiateHandoverRequest.class);
        clientHsResponseConnector.publish(new ServerHandshakeResponseEvent(infrastructure.eventSourceId, OkResponseInitHandover));
        clientHsNotificationConnector.publish(new ServerHandshakeStatusUpdateEvent(infrastructure.eventSourceId, ServerSideStates.READY_LOADED));

        serverHsProxy.expectMsgClass(StartHandoverRequest.class);
        clientHsResponseConnector.publish(new ServerHandshakeResponseEvent(infrastructure.eventSourceId, OkResponseStartHandover));

        conveyorProbe.expectMsgAnyClassOf(Duration.ofSeconds(3), LoadConveyorRequest.class, UnloadConveyorRequest.class);

        expectCoordinatorState(BasicMachineStates.COMPLETING);
        expectCoordinatorState(BasicMachineStates.COMPLETE);
    }


    private MachineTestChildFUs initializeTurntableInfraStructureWithAttachedProxies() {
        MachineTestChildFUs fus = new MachineTestChildFUs();

        intraMachineProbe = new TestKit(infrastructure.getSystem());
        intraMachineEventBus().subscribe(intraMachineProbe.getRef(), infrastructure.getTestClassifier());

        turningProbe = new TestKit(infrastructure.getSystem());
        conveyorProbe = new TestKit(infrastructure.getSystem());
        serverHsProbe = new TestKit(infrastructure.getSystem());
        clientHsProbe = new TestKit(infrastructure.getSystem());

        serverHsResponseConnector = new ServerResponseConnector();
        serverHsNotificationConnector = new ServerNotificationConnector();

        clientRemoteConnector = new FUConnector();
        clientHsResponseConnector = new ServerResponseConnector();
        clientHsNotificationConnector = new ServerNotificationConnector();

        serverHsProxy = new TestKit(infrastructure.getSystem());
        clientRemoteConnector.subscribe(serverHsProxy.getRef(), infrastructure.getTestClassifier());

        turningConnector = fus.addTurningFU(turningProbe, infrastructure.getTestClassifier());
        conveyorConnector = fus.addConveyorFU(conveyorProbe, infrastructure.getTestClassifier());
        serverHsConnector = fus.addServerSideHandshake(TRANSPORT_MODULE_NORTH_SERVER, serverHsProbe,
                infrastructure.getTestClassifier(), serverHsResponseConnector, serverHsNotificationConnector);
        clientHsConnector = fus.addClientSideHandshake(TRANSPORT_MODULE_SOUTH_CLIENT, clientHsProbe,
                infrastructure.getTestClassifier(), clientRemoteConnector, clientHsResponseConnector, clientHsNotificationConnector);
        return fus;
    }

    private void initializeFUsAndCheckInitialState() {
        //Simulate later creation to mimic creation from inside actor
        ActorRef actor = infrastructure.getSystem().actorOf(TurningActor.props(turningConnector, intraMachineEventBus()), "TurningFU");
        intraMachineProbe.expectMsgClass(TurningStatusUpdateEvent.class);
        fuActors.add(actor);

        actor = infrastructure.getSystem().actorOf(ConveyorActor.props(conveyorConnector, intraMachineEventBus()), "ConveyorFU");
        intraMachineProbe.expectMsgClass(ConveyorStatusUpdateEvent.class);
        fuActors.add(actor);

        actor = infrastructure.getSystem().actorOf(ServerSideHandshakeActor.props(serverHsConnector, intraMachineEventBus(),
                serverHsResponseConnector, serverHsNotificationConnector), TRANSPORT_MODULE_NORTH_SERVER);
        intraMachineProbe.expectMsgClass(ServerHandshakeStatusUpdateEvent.class);
        fuActors.add(actor);

        actor = infrastructure.getSystem().actorOf(ClientSideHandshakeActor.propsLocalHandshake(clientHsConnector, intraMachineEventBus(),
                clientRemoteConnector, clientHsResponseConnector, clientHsNotificationConnector), TRANSPORT_MODULE_SOUTH_CLIENT);
        intraMachineProbe.expectMsgClass(ClientHandshakeStatusUpdateEvent.class);
        fuActors.add(actor);
    }

    private void unsubscribeFromAllConnectors() {
        intraMachineEventBus().unsubscribe(intraMachineProbe.getRef());
        turningConnector.unsubscribe(turningProbe.getRef());
        conveyorConnector.unsubscribe(conveyorProbe.getRef());
        serverHsConnector.unsubscribe(serverHsProbe.getRef());
        clientHsConnector.unsubscribe(clientHsProbe.getRef());
        clientRemoteConnector.unsubscribe(serverHsProxy.getRef());
        for (ActorRef actorRef : fuActors) {
            infrastructure.getSystem().stop(actorRef);
        }
    }

    private void expectCoordinatorState(BasicMachineStates coordinatorState) {
        MachineStatusUpdateEvent machineStatusUpdateEvent = getProbe().expectMsgClass(Duration.ofSeconds(10), MachineStatusUpdateEvent.class);
        assertEquals(coordinatorState, machineStatusUpdateEvent.getStatus());
    }

    private TestKit getProbe() {
        return infrastructure.getProbe();
    }

    private ActorRef actorRef() {
        return infrastructure.getActorRef();
    }

    private IntraMachineEventBus intraMachineEventBus() {
        return infrastructure.getIntraMachineEventBus();
    }

}
