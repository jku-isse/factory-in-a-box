package functionalUnits.functionalUnitMock;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import stateMachines.conveyor.ConveyorStates;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

//TODO create tests for illegal paths of state machine
public class ConveyorMockTest {

    private ConveyorMock conveyorMock;
    private static long timerMs;

    @BeforeAll
    static void init() {
        timerMs = 200;
    }   //reducing this value may impact awaitility

    @BeforeEach
    void buildTest() {
        conveyorMock = new ConveyorMock(timerMs);
        checkValidStoppedState();
        conveyorMock.reset();
        checkValidResetState();
        await().until(() -> conveyorMock.getConveyorStateMachine().isInState(ConveyorStates.IDLE));
        checkValidIdleState();
    }

    /*
     *  ================================== Correct transitions ================================== *
     */

    @Test
    void testIdleAtStartSuccessful(){
        checkValidIdleState();
    }

    @Test
    void testLoadedSuccessful() {
        conveyorMock.load();
        checkValidLoadingState();
        await().atMost(timerMs * 2, TimeUnit.MILLISECONDS)
                .until(() -> conveyorMock.getConveyorStateMachine().isInState(ConveyorStates.FULLY_OCCUPIED));
        checkValidFullyOccupiedState();
    }

    @Test
    void testEmptyUnloadedFail() {
        conveyorMock.unload();
        checkValidIdleState();
    }

    @Test
    void testLoadThenUnloadSuccessful() {
        conveyorMock.load();
        await().atMost(timerMs * 2, TimeUnit.MILLISECONDS)
                .until(() -> conveyorMock.getConveyorStateMachine().isInState(ConveyorStates.FULLY_OCCUPIED));
        conveyorMock.unload();
        checkValidUnloadingState();
        await().atMost(timerMs * 2, TimeUnit.MILLISECONDS)
                .until(() -> conveyorMock.getConveyorStateMachine().isInState(ConveyorStates.IDLE));
        checkValidIdleState();
    }

    @Test
    void testPauseFromLoadingSuccessful() {
        conveyorMock.load();
        checkValidLoadingState();
        conveyorMock.pause();
        checkValidSuspendedState();
    }

    @Test
    void testPauseFromUnloadingSuccessful() {
        conveyorMock.load();
        checkValidLoadingState();
        await().until(() -> conveyorMock.getConveyorStateMachine().isInState(ConveyorStates.FULLY_OCCUPIED));
        conveyorMock.unload();
        conveyorMock.pause();
        checkValidSuspendedState();
    }

    @Test
    void testLoadingFromSuspendedSuccessful() {
        conveyorMock.load();
        checkValidLoadingState();
        conveyorMock.pause();
        checkValidSuspendedState();
        conveyorMock.load();
        checkValidLoadingState();
        await().until(() -> conveyorMock.getConveyorStateMachine().isInState(ConveyorStates.FULLY_OCCUPIED));
        checkValidFullyOccupiedState();
    }

    @Test
    void testUnloadingFromSuspendedSuccessful() {
        conveyorMock.load();
        checkValidLoadingState();
        conveyorMock.pause();
        checkValidSuspendedState();
        conveyorMock.unload();
        checkValidUnloadingState();
        await().until(() -> conveyorMock.getConveyorStateMachine().isInState(ConveyorStates.IDLE));
        checkValidIdleState();
    }

    @Test
    void testSuspendFromResettingSuccessful() {
        conveyorMock.stop();
        await().until(() -> conveyorMock.getConveyorStateMachine().isInState(ConveyorStates.STOPPED));
        checkValidStoppedState();
        conveyorMock.reset();
        checkValidResetState();
        conveyorMock.pause();
        await().until(() -> conveyorMock.getConveyorStateMachine().isInState(ConveyorStates.SUSPENDED));
        checkValidSuspendedState();
    }

    @Test
    void testStopFromIdleSuccessful() {
        conveyorMock.stop();
        checkValidStoppingState();
        await().until(() -> conveyorMock.getConveyorStateMachine().isInState(ConveyorStates.STOPPED));
        checkValidStoppedState();
    }

    @Test
    void testStopFromLoadingSuccessful() {
        conveyorMock.load();
        await().until(() -> conveyorMock.getConveyorStateMachine().isInState(ConveyorStates.FULLY_OCCUPIED));
        checkValidFullyOccupiedState();
        conveyorMock.stop();
        checkValidStoppingState();
        await().until(() -> conveyorMock.getConveyorStateMachine().isInState(ConveyorStates.STOPPED));
        checkValidStoppedState();
    }

    @Test
    void testStopFromUnloadingSuccessful() {
        conveyorMock.load();
        await().until(() -> conveyorMock.getConveyorStateMachine().isInState(ConveyorStates.FULLY_OCCUPIED));
        checkValidFullyOccupiedState();
        conveyorMock.unload();
        checkValidUnloadingState();
        await().until(() -> conveyorMock.getConveyorStateMachine().isInState(ConveyorStates.IDLE));
        checkValidIdleState();
        conveyorMock.stop();
        checkValidStoppingState();
        await().until(() -> conveyorMock.getConveyorStateMachine().isInState(ConveyorStates.STOPPED));
        checkValidStoppedState();
    }

    @Test
    void testStopFromFullyOccupiedSuccessful() {
        conveyorMock.load();
        await().until(() -> conveyorMock.getConveyorStateMachine().isInState(ConveyorStates.FULLY_OCCUPIED));
        checkValidFullyOccupiedState();
        conveyorMock.stop();
        checkValidStoppingState();
        await().until(() -> conveyorMock.getConveyorStateMachine().isInState(ConveyorStates.STOPPED));
        checkValidStoppedState();
    }

    @Test
    void testStopFromSuspendedSuccessful() {
        conveyorMock.load();
        checkValidLoadingState();
        conveyorMock.pause();
        checkValidSuspendedState();
        conveyorMock.stop();
        checkValidStoppingState();
        await().until(() -> conveyorMock.getConveyorStateMachine().isInState(ConveyorStates.STOPPED));
        checkValidStoppedState();
    }

    @Test
    void testStopFromResetSuccessful() {
        conveyorMock.stop();
        await().until(() -> conveyorMock.getConveyorStateMachine().isInState(ConveyorStates.STOPPED));
        conveyorMock.reset();
        checkValidResetState();
        conveyorMock.stop();
        checkValidStoppingState();
        await().until(() -> conveyorMock.getConveyorStateMachine().isInState(ConveyorStates.STOPPED));
        checkValidStoppedState();
    }


    /*
     *  ================================= Incorrect transitions ================================= *
     */
    @Test
    void testLoadingTwiceFail() {
        conveyorMock.load();
        checkValidLoadingState();
        await().atMost(timerMs * 2, TimeUnit.MILLISECONDS)
                .until(() -> conveyorMock.getConveyorStateMachine().isInState(ConveyorStates.FULLY_OCCUPIED));
        checkValidFullyOccupiedState();
        conveyorMock.load();
        checkValidFullyOccupiedState();
    }


    /*
     *  ================================= State Machine validation =============================== *
     */
    void checkValidIdleState() {
        assertEquals(ConveyorStates.IDLE, conveyorMock.getConveyorStateMachine().getState());
        assertFalse(conveyorMock.getConveyorMotor().isRunning());
        assertFalse(conveyorMock.getSensorLoading().hasDetectedInput());
        assertFalse(conveyorMock.getSensorUnloading().hasDetectedInput());
    }

    void checkValidLoadingState() {
        assertEquals(ConveyorStates.LOADING, conveyorMock.getConveyorStateMachine().getState());
        assertTrue(conveyorMock.getConveyorMotor().isRunning());
        assertFalse(conveyorMock.getSensorLoading().hasDetectedInput());
        assertTrue(conveyorMock.getSensorUnloading().hasDetectedInput());
        assertFalse(conveyorMock.getFullyUnloaded().get());
    }

    void checkValidUnloadingState() {
        assertEquals(ConveyorStates.UNLOADING, conveyorMock.getConveyorStateMachine().getState());
        assertTrue(conveyorMock.getConveyorMotor().isRunning());
        assertFalse(conveyorMock.getSensorLoading().hasDetectedInput());
    }

    void checkValidSuspendedState() {
        assertEquals(ConveyorStates.SUSPENDED, conveyorMock.getConveyorStateMachine().getState());
        assertFalse(conveyorMock.getConveyorMotor().isRunning());
    }

    void checkValidResetState() {
        assertEquals(ConveyorStates.RESETTING, conveyorMock.getConveyorStateMachine().getState());
        assertFalse(conveyorMock.getConveyorMotor().isRunning());
    }

    void checkValidFullyOccupiedState() {
        assertEquals(ConveyorStates.FULLY_OCCUPIED, conveyorMock.getConveyorStateMachine().getState());
        assertFalse(conveyorMock.getConveyorMotor().isRunning());
        assertTrue(conveyorMock.getSensorLoading().hasDetectedInput());
        assertTrue(conveyorMock.getSensorUnloading().hasDetectedInput());
        assertTrue(conveyorMock.getFullyLoaded().get());
        assertFalse(conveyorMock.getFullyUnloaded().get());
    }

    void checkValidStoppingState() {
        assertEquals(ConveyorStates.STOPPING, conveyorMock.getConveyorStateMachine().getState());
        assertFalse(conveyorMock.getConveyorMotor().isRunning());
    }

    void checkValidStoppedState() {
        assertEquals(ConveyorStates.STOPPED, conveyorMock.getConveyorStateMachine().getState());
        assertFalse(conveyorMock.getConveyorMotor().isRunning());
    }
}
