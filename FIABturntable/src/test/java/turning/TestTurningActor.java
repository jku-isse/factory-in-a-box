package turning;

import akka.actor.ActorRef;
import akka.testkit.javadsl.TestKit;
import testutils.ActorTestInfrastructure;
import fiab.core.capabilities.functionalunit.ResetRequest;
import fiab.core.capabilities.functionalunit.StopRequest;
import fiab.functionalunit.connector.FUConnector;
import fiab.core.capabilities.transport.TransportDestinations;
import fiab.turntable.turning.TurningActor;
import fiab.turntable.turning.messages.TurnRequest;
import fiab.turntable.turning.messages.TurningStatusUpdateEvent;
import fiab.turntable.turning.statemachine.TurningStates;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("UnitTest")
public class TestTurningActor {

    private static ActorTestInfrastructure infrastructure;

    @BeforeEach
    public void init() {
        infrastructure = new ActorTestInfrastructure();
        infrastructure.subscribeToIntraMachineEventBus();
        FUConnector turningConnector = new FUConnector();
        infrastructure.initializeActor(TurningActor.props(turningConnector,infrastructure.getIntraMachineEventBus()),
                "Turning"+infrastructure.getAndIncrementRunCount());
        expectTurningState(TurningStates.STOPPED);
    }

    @AfterEach
    public void teardown() {
        infrastructure.destroyActor();
        infrastructure.shutdownInfrastructure();
    }

    @Test
    public void testAllValidTransitionsSuccess() {
        new TestKit(infrastructure.getSystem()) {
            {
                actorRef().tell(new ResetRequest(infrastructure.eventSourceId), ActorRef.noSender());
                expectTurningState(TurningStates.RESETTING);
                expectTurningState(TurningStates.IDLE);

                actorRef().tell(new TurnRequest(infrastructure.eventSourceId, TransportDestinations.SOUTH),
                        ActorRef.noSender());
                expectTurningState(TurningStates.STARTING);
                expectTurningState(TurningStates.EXECUTING);
                expectTurningState(TurningStates.COMPLETING);
                expectTurningState(TurningStates.COMPLETE);

                actorRef().tell(new StopRequest(infrastructure.eventSourceId), ActorRef.noSender());
                expectTurningState(TurningStates.STOPPING);
                expectTurningState(TurningStates.STOPPED);
            }
        };
    }

    @Test
    public void testInvalidTransitionFail() {
        new TestKit(infrastructure.getSystem()) {
            {
                actorRef().tell(new TurnRequest(infrastructure.eventSourceId, TransportDestinations.NORTH),
                        ActorRef.noSender());
                expectNoMessage();
            }
        };
    }

    @Test
    public void testResetFromIdleTransitionFail() {
        new TestKit(infrastructure.getSystem()) {
            {
                actorRef().tell(new ResetRequest(infrastructure.eventSourceId), ActorRef.noSender());
                expectTurningState(TurningStates.RESETTING);
                expectTurningState(TurningStates.IDLE);
                // Now that we are in idle, check that we stay in idle
                actorRef().tell(new ResetRequest(infrastructure.eventSourceId), ActorRef.noSender());
                expectNoMessage();
            }
        };
    }

    @Test
    public void testTurnToNorthSuccess() {
        resetTurningActor();
        actorRef().tell(new TurnRequest(infrastructure.eventSourceId, TransportDestinations.NORTH),
                ActorRef.noSender());
        expectTurningState(TurningStates.STARTING);
        expectTurningState(TurningStates.EXECUTING);
        expectTurningState(TurningStates.COMPLETING);
        expectTurningState(TurningStates.COMPLETE);
    }

    @Test
    public void testTurnToEastSuccess() {
        resetTurningActor();
        actorRef().tell(new TurnRequest(infrastructure.eventSourceId, TransportDestinations.EAST),
                ActorRef.noSender());
        expectTurningState(TurningStates.STARTING);
        expectTurningState(TurningStates.EXECUTING);
        expectTurningState(TurningStates.COMPLETING);
        expectTurningState(TurningStates.COMPLETE);
    }

    @Test
    public void testTurnToSouthSuccess() {
        resetTurningActor();
        actorRef().tell(new TurnRequest(infrastructure.eventSourceId, TransportDestinations.SOUTH),
                ActorRef.noSender());
        expectTurningState(TurningStates.STARTING);
        expectTurningState(TurningStates.EXECUTING);
        expectTurningState(TurningStates.COMPLETING);
        expectTurningState(TurningStates.COMPLETE);
    }

    @Test
    public void testTurnToWestSuccess() {
        resetTurningActor();
        actorRef().tell(new TurnRequest(infrastructure.eventSourceId,TransportDestinations.WEST),
                ActorRef.noSender());
        expectTurningState(TurningStates.STARTING);
        expectTurningState(TurningStates.EXECUTING);
        expectTurningState(TurningStates.COMPLETING);
        expectTurningState(TurningStates.COMPLETE);
    }

    private void resetTurningActor() {
        actorRef().tell(new ResetRequest(infrastructure.eventSourceId), ActorRef.noSender());
        expectTurningState(TurningStates.RESETTING);
        expectTurningState(TurningStates.IDLE);
    }

    private void expectTurningState(TurningStates turningStates) {
        TurningStatusUpdateEvent machineStatusUpdateEvent = probe().expectMsgClass(TurningStatusUpdateEvent.class);
        assertEquals(machineStatusUpdateEvent.getStatus(), turningStates);
    }

    private TestKit probe() {
        return infrastructure.getProbe();
    }

    private ActorRef actorRef() {
        return infrastructure.getActorRef();
    }


}
