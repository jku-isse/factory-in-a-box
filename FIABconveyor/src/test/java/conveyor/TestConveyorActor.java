package conveyor;

import akka.actor.ActorRef;
import akka.testkit.javadsl.TestKit;

import fiab.conveyor.ConveyorActor;
import fiab.conveyor.messages.ConveyorStatusUpdateEvent;
import fiab.conveyor.messages.LoadConveyorRequest;
import fiab.conveyor.messages.UnloadConveyorRequest;
import fiab.conveyor.statemachine.ConveyorStates;
import fiab.core.capabilities.functionalunit.ResetRequest;
import fiab.core.capabilities.functionalunit.StopRequest;
import fiab.functionalunit.connector.FUConnector;
import org.junit.jupiter.api.*;
import testutils.ActorTestInfrastructure;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("UnitTest")
public class TestConveyorActor {

    private static ActorTestInfrastructure infrastructure;
    private static FUConnector conveyorConnector;

    @BeforeAll
    static void setup() {
        infrastructure = new ActorTestInfrastructure();
        infrastructure.subscribeToIntraMachineEventBus();
        conveyorConnector = new FUConnector();
    }

    @BeforeEach
    void init() {
        infrastructure.initializeActor(ConveyorActor.props(conveyorConnector, infrastructure.getIntraMachineEventBus()),
                "ConveyorActor" + infrastructure.getAndIncrementRunCount());
        expectConveyorState(ConveyorStates.STOPPED);    //The conveyor always starts in stopped
    }

    @AfterEach
    void teardown() {
        infrastructure.destroyActor();
    }

    @AfterAll
    static void cleanup() {
        infrastructure.shutdownInfrastructure();
    }

    @Test
    public void testAllValidTransitionsSuccess() {
        new TestKit(infrastructure.getSystem()) {
            {
                actorRef().tell(new ResetRequest(infrastructure.eventSourceId), ActorRef.noSender());
                expectConveyorState(ConveyorStates.RESETTING);
                expectConveyorState(ConveyorStates.IDLE_EMPTY);

                actorRef().tell(new LoadConveyorRequest(infrastructure.eventSourceId), ActorRef.noSender());
                expectConveyorState(ConveyorStates.LOADING);
                expectConveyorState(ConveyorStates.IDLE_FULL);

                actorRef().tell(new UnloadConveyorRequest(infrastructure.eventSourceId), ActorRef.noSender());
                expectConveyorState(ConveyorStates.UNLOADING);
                expectConveyorState(ConveyorStates.IDLE_EMPTY);

                actorRef().tell(new StopRequest(infrastructure.eventSourceId), ActorRef.noSender());
                expectConveyorState(ConveyorStates.STOPPING);
                expectConveyorState(ConveyorStates.STOPPED);
            }
        };
    }

    @Test
    public void testInvalidTransitionFail() {
        new TestKit(infrastructure.getSystem()) {
            {
                actorRef().tell(new LoadConveyorRequest(infrastructure.eventSourceId), ActorRef.noSender());
                expectNoMessage();
            }
        };
    }

    @Test
    public void testResetFromIdleTransitionFail() {
        new TestKit(infrastructure.getSystem()) {
            {
                actorRef().tell(new ResetRequest(infrastructure.eventSourceId), ActorRef.noSender());
                expectConveyorState(ConveyorStates.RESETTING);
                expectConveyorState(ConveyorStates.IDLE_EMPTY);
                // Now that we are in idle, check that we stay in idle
                actorRef().tell(new ResetRequest(infrastructure.eventSourceId), ActorRef.noSender());
                expectNoMessage();
            }
        };
    }

    private void expectConveyorState(ConveyorStates conveyorState) {
        ConveyorStatusUpdateEvent machineStatusUpdateEvent = getProbe().expectMsgClass(Duration.ofSeconds(30), ConveyorStatusUpdateEvent.class);
        assertEquals(machineStatusUpdateEvent.getStatus(), conveyorState);
    }

    private TestKit getProbe() {
        return infrastructure.getProbe();
    }

    private ActorRef actorRef() {
        return infrastructure.getActorRef();
    }
}
