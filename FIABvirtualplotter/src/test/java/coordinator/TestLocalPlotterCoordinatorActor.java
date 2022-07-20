package coordinator;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import coordinator.infrastructure.PlotterTestChildFUs;
import fiab.conveyor.ConveyorActor;
import fiab.conveyor.messages.ConveyorStatusUpdateEvent;
import fiab.conveyor.messages.LoadConveyorRequest;
import fiab.conveyor.messages.UnloadConveyorRequest;
import fiab.conveyor.statemachine.ConveyorStates;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.functionalunit.ResetRequest;
import fiab.core.capabilities.functionalunit.StopRequest;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.core.capabilities.handshake.client.CompleteHandshake;
import fiab.core.capabilities.handshake.server.InitiateHandoverRequest;
import fiab.core.capabilities.handshake.server.StartHandoverRequest;
import fiab.core.capabilities.handshake.server.TransportAreaStatusOverrideRequest;
import fiab.core.capabilities.plotting.PlotRequest;
import fiab.functionalunit.connector.FUConnector;
import fiab.functionalunit.connector.FUSubscriptionClassifier;
import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.functionalunit.connector.MachineEventBus;
import fiab.handshake.connector.ServerNotificationConnector;
import fiab.handshake.connector.ServerResponseConnector;
import fiab.handshake.server.ServerSideHandshakeActor;
import fiab.handshake.server.messages.ServerHandshakeStatusUpdateEvent;
import fiab.plotter.PlotterCoordinatorActor;
import fiab.plotter.plotting.PlotterActor;
import fiab.plotter.plotting.message.PlottingStatusUpdateEvent;
import org.junit.jupiter.api.*;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestLocalPlotterCoordinatorActor {

    private ActorSystem system;
    private IntraMachineEventBus intraMachineEventBus;
    private TestKit intraMachineProbe;
    private String intraMachineProbeId;

    private FUSubscriptionClassifier testClassifier;

    private TestKit machineProbe;
    private String machineProbeId;

    private TestKit plottingProbe;
    private String plottingProbeId;
    private TestKit conveyorProbe;
    private String conveyorProbeId;
    private TestKit handshakeProbe;
    private String handshakeProbeId;

    private ActorRef plotter;
    private ActorRef handshake;

    @BeforeEach
    public void setup() {
        system = ActorSystem.create();
        MachineEventBus machineEventBus = new MachineEventBus();
        intraMachineEventBus = new IntraMachineEventBus();
        intraMachineProbe = new TestKit(system);
        intraMachineProbeId = intraMachineProbe.getRef().path().name();
        intraMachineEventBus.subscribe(intraMachineProbe.getRef(), new FUSubscriptionClassifier(intraMachineProbeId, "*"));

        testClassifier = new FUSubscriptionClassifier("TestSystem", "*");

        plottingProbe = new TestKit(system);
        plottingProbeId = plottingProbe.getRef().path().name();
        conveyorProbe = new TestKit(system);
        conveyorProbeId = conveyorProbe.getRef().path().name();
        handshakeProbe = new TestKit(system);
        handshakeProbeId = handshakeProbe.getRef().path().name();
        machineProbe = new TestKit(system);
        machineProbeId = machineProbe.getRef().path().name();
        machineEventBus.subscribe(machineProbe.getRef(), new FUSubscriptionClassifier(machineProbeId, "*"));

        PlotterTestChildFUs fus = initializeChildFUs();
        plotter = system.actorOf(PlotterCoordinatorActor.props(machineEventBus, intraMachineEventBus, fus), "Plotter");
        MachineStatusUpdateEvent event = machineProbe.expectMsgClass(MachineStatusUpdateEvent.class);
        assertEquals(BasicMachineStates.STOPPED, event.getStatus());

    }

    @AfterEach
    public void teardown() {
        TestKit.shutdownActorSystem(system);
    }

    @Test
    public void testResetAndStopPlotter() {
        plotter.tell(new ResetRequest(machineProbeId), machineProbe.getRef());
        expectMachineState(BasicMachineStates.RESETTING);
        conveyorProbe.expectMsgClass(ResetRequest.class);
        plottingProbe.expectMsgClass(ResetRequest.class);
        expectMachineState(BasicMachineStates.IDLE);

        plotter.tell(new StopRequest(machineProbeId), machineProbe.getRef());
        expectMachineState(BasicMachineStates.STOPPING);
        conveyorProbe.expectMsgClass(StopRequest.class);
        plottingProbe.expectMsgClass(ResetRequest.class);   //Where does this second reset come from?
        plottingProbe.expectMsgClass(StopRequest.class);
        handshakeProbe.expectMsgClass(StopRequest.class);
        expectMachineState(BasicMachineStates.STOPPED);
    }

    @Test
    public void testPlotterFullCycle() {
        plotter.tell(new ResetRequest(machineProbeId), machineProbe.getRef());
        expectMachineState(BasicMachineStates.RESETTING);
        plottingProbe.expectMsgClass(ResetRequest.class);
        conveyorProbe.expectMsgClass(ResetRequest.class);
        expectMachineState(BasicMachineStates.IDLE);

        plotter.tell(new PlotRequest(machineProbeId, "TestId"), ActorRef.noSender());
        expectMachineState(BasicMachineStates.STARTING);

        handshakeProbe.expectMsgClass(ResetRequest.class);
        intraMachineProbe.fishForMessage(Duration.ofSeconds(3), "Wait for server handshake to reset",
                msg -> msg instanceof ServerHandshakeStatusUpdateEvent &&
                ((ServerHandshakeStatusUpdateEvent) msg).getStatus() == ServerSideStates.RESETTING);
        intraMachineProbe.fishForMessage(Duration.ofSeconds(3), "Wait for server handshake to reach idle",
                msg -> msg instanceof ServerHandshakeStatusUpdateEvent
                        &&((ServerHandshakeStatusUpdateEvent) msg).getStatus() == ServerSideStates.IDLE_EMPTY);
        handshake.tell(new InitiateHandoverRequest(handshakeProbeId), handshakeProbe.getRef());
        handshake.tell(new StartHandoverRequest(handshakeProbeId), handshakeProbe.getRef());

        conveyorProbe.expectMsgClass(LoadConveyorRequest.class);
        handshakeProbe.expectMsgClass(TransportAreaStatusOverrideRequest.class);

        handshakeProbe.expectMsgClass(CompleteHandshake.class);
        intraMachineProbe.fishForMessage(Duration.ofSeconds(3), "Wait for server handshake to complete",
                msg -> msg instanceof ServerHandshakeStatusUpdateEvent
                        && ((ServerHandshakeStatusUpdateEvent) msg).getStatus() == ServerSideStates.COMPLETE);

        expectMachineState(BasicMachineStates.EXECUTE);
        expectMachineState(BasicMachineStates.COMPLETING);

        handshakeProbe.expectMsgClass(ResetRequest.class);
        intraMachineProbe.fishForMessage(Duration.ofSeconds(3), "Wait for server handshake to reset",
                msg -> msg instanceof ServerHandshakeStatusUpdateEvent &&
                        ((ServerHandshakeStatusUpdateEvent) msg).getStatus() == ServerSideStates.RESETTING);
        intraMachineProbe.fishForMessage(Duration.ofSeconds(3), "Wait for server handshake to reach idle",
                msg -> msg instanceof ServerHandshakeStatusUpdateEvent
                        &&((ServerHandshakeStatusUpdateEvent) msg).getStatus() == ServerSideStates.IDLE_LOADED);
        handshake.tell(new InitiateHandoverRequest(handshakeProbeId), handshakeProbe.getRef());
        handshake.tell(new StartHandoverRequest(handshakeProbeId), handshakeProbe.getRef());
        conveyorProbe.expectMsgClass(UnloadConveyorRequest.class);

        expectMachineState(BasicMachineStates.COMPLETE);
    }

    private void expectMachineState(BasicMachineStates state) {
        MachineStatusUpdateEvent event = machineProbe.expectMsgClass(Duration.ofSeconds(15), MachineStatusUpdateEvent.class);
        assertEquals(state, event.getStatus());
    }

    private PlotterTestChildFUs initializeChildFUs() {
        PlotterTestChildFUs fus = new PlotterTestChildFUs();

        FUSubscriptionClassifier plottingSubscriptionClassifier = new FUSubscriptionClassifier(plottingProbeId, "*");
        FUConnector plottingFUBus = fus.addPlottingFU(plottingProbe, plottingSubscriptionClassifier);
        plottingFUBus.subscribe(plottingProbe.getRef(), plottingSubscriptionClassifier);
        system.actorOf(PlotterActor.props(plottingFUBus, intraMachineEventBus), "PlottingFU");
        PlottingStatusUpdateEvent plottingStatusUpdate = intraMachineProbe.expectMsgClass(PlottingStatusUpdateEvent.class);
        assertEquals(BasicMachineStates.STOPPED, plottingStatusUpdate.getStatus());

        FUSubscriptionClassifier conveyorSubscriptionClassifier = new FUSubscriptionClassifier(conveyorProbeId, "*");
        FUConnector conveyorFUBus = fus.addConveyorFU(conveyorProbe, conveyorSubscriptionClassifier);
        conveyorFUBus.subscribe(plottingProbe.getRef(), conveyorSubscriptionClassifier);
        system.actorOf(ConveyorActor.props(conveyorFUBus, intraMachineEventBus), "ConveyorFU");
        ConveyorStatusUpdateEvent conveyorStatusUpdate = intraMachineProbe.expectMsgClass(ConveyorStatusUpdateEvent.class);
        assertEquals(ConveyorStates.STOPPED, conveyorStatusUpdate.getStatus());

        FUSubscriptionClassifier handshakeSubscriptionClassifier = new FUSubscriptionClassifier(handshakeProbeId, "*");
        FUConnector handshakeFUBus = fus.addServerSideHandshake(handshakeProbe, handshakeSubscriptionClassifier);
        handshakeFUBus.subscribe(handshakeProbe.getRef(), handshakeSubscriptionClassifier);
        handshake = system.actorOf(ServerSideHandshakeActor.props(handshakeFUBus, intraMachineEventBus,
                new ServerResponseConnector(), new ServerNotificationConnector()), HandshakeCapability.SERVER_CAPABILITY_ID);
        ServerHandshakeStatusUpdateEvent serverHandshakeStatusUpdate = intraMachineProbe.expectMsgClass(ServerHandshakeStatusUpdateEvent.class);
        assertEquals(ServerSideStates.STOPPED, serverHandshakeStatusUpdate.getStatus());

        return fus;
    }
}
