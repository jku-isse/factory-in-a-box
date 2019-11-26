package functionalUnits.functionalUnitTurnTable;

import hardware.ConveyorMockHardware;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import stateMachines.conveyor.ConveyorStates;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static stateMachines.conveyor.ConveyorStates.*;

public class ConveyorTurnTableTest {

    private static int speed;
    private static long delay;
    private ConveyorMockHardware conveyorMockHardware;
    private ConveyorTurnTable conveyorTurnTable;
    private int historyIndex;

    @BeforeAll
    static void init() {
        speed = 100;
        delay = 500;
    }

    @BeforeEach
    void buildTest() {
        historyIndex = 0;
        conveyorMockHardware = new ConveyorMockHardware(speed, delay);
        conveyorTurnTable = new ConveyorTurnTable(conveyorMockHardware.getConveyorMockMotor(),
                conveyorMockHardware.getMockSensorLoading(), conveyorMockHardware.getMockSensorUnloading());
        assertFalse(conveyorMockHardware.getConveyorMockMotor().isRunning());
        assertFalse(conveyorMockHardware.getMockSensorLoading().hasDetectedInput());
        assertFalse(conveyorMockHardware.getMockSensorUnloading().hasDetectedInput());
        assertEquals(STOPPED, conveyorTurnTable.getConveyorStateMachine().getState());
        conveyorTurnTable.reset();
        await().until(() -> conveyorTurnTable.getConveyorStateMachine().isInState(IDLE));

        checkLogValue(RESETTING);
        checkNextLogValue(IDLE);
    }

    @Test
    void checkIdleOnStartSuccessful() {
        checkLogValue(IDLE);
    }

    @Test
    void testLoadedSuccessful() {
        conveyorTurnTable.load();
        await().until(() -> conveyorTurnTable.getConveyorStateMachine().isInState(FULLY_OCCUPIED));

        checkNextLogValue(LOADING);
        checkNextLogValue(FULLY_OCCUPIED);
    }

    @Test
    void testEmptyUnloadedFail() {
        conveyorTurnTable.unload();
        checkLogValue(IDLE);
    }

    @Test
    void testLoadThenUnloadSuccessful() {
        conveyorTurnTable.load();
        await().until(() -> conveyorTurnTable.getConveyorStateMachine().isInState(ConveyorStates.FULLY_OCCUPIED));
        conveyorTurnTable.unload();
        await().until(() -> conveyorTurnTable.getConveyorStateMachine().isInState(ConveyorStates.IDLE));

        checkNextLogValue(LOADING);
        checkNextLogValue(FULLY_OCCUPIED);
        checkNextLogValue(UNLOADING);
        checkNextLogValue(IDLE);
    }

    @Test
    void testPauseFromLoadingSuccessful() {
        conveyorTurnTable.load();
        conveyorTurnTable.pause();

        checkNextLogValue(LOADING);
        checkNextLogValue(SUSPENDED);
    }

    @Test
    void testPauseFromUnloadingSuccessful() {
        conveyorTurnTable.load();
        await().until(() -> conveyorTurnTable.getConveyorStateMachine().isInState(ConveyorStates.FULLY_OCCUPIED));
        conveyorTurnTable.unload();
        await().until(() -> conveyorTurnTable.getConveyorStateMachine().isInState(UNLOADING));
        conveyorTurnTable.pause();

        checkNextLogValue(LOADING);
        checkNextLogValue(FULLY_OCCUPIED);
        checkNextLogValue(UNLOADING);
        checkNextLogValue(SUSPENDED);
    }

    @Test
    void testLoadingFromSuspendedSuccessful() {
        conveyorTurnTable.load();
        conveyorTurnTable.pause();
        conveyorTurnTable.load();
        await().until(() -> conveyorTurnTable.getConveyorStateMachine().isInState(ConveyorStates.FULLY_OCCUPIED));

        checkNextLogValue(LOADING);
        checkNextLogValue(SUSPENDED);
        checkNextLogValue(LOADING);
        checkNextLogValue(FULLY_OCCUPIED);
    }

    @Test
    void testUnloadingFromSuspendedSuccessful() {
        conveyorTurnTable.load();
        conveyorTurnTable.pause();
        conveyorTurnTable.unload();
        await().until(() -> conveyorTurnTable.getConveyorStateMachine().isInState(ConveyorStates.IDLE));

        checkNextLogValue(LOADING);
        checkNextLogValue(SUSPENDED);
        checkNextLogValue(UNLOADING);
        checkNextLogValue(IDLE);
    }

    @Test
    void testSuspendFromResettingSuccessful() {
        conveyorTurnTable.stop();
        await().until(() -> conveyorTurnTable.getConveyorStateMachine().isInState(ConveyorStates.STOPPED));
        conveyorTurnTable.reset();
        conveyorTurnTable.pause();

        checkNextLogValue(STOPPING);
        checkNextLogValue(STOPPED);
        checkNextLogValue(RESETTING);
        checkNextLogValue(SUSPENDED);
    }

    @Test
    void testStopFromIdleSuccessful() {
        conveyorTurnTable.stop();
        await().until(() -> conveyorTurnTable.getConveyorStateMachine().isInState(ConveyorStates.STOPPED));

        checkNextLogValue(STOPPING);
        checkNextLogValue(STOPPED);
    }

    @Test
    void testStopFromLoadingSuccessful() {
        conveyorTurnTable.load();
        await().until(() -> conveyorTurnTable.getConveyorStateMachine().isInState(FULLY_OCCUPIED));
        conveyorTurnTable.stop();
        await().until(() -> conveyorTurnTable.getConveyorStateMachine().isInState(STOPPED));

        checkNextLogValue(LOADING);
        checkNextLogValue(FULLY_OCCUPIED);
        checkNextLogValue(STOPPING);
        checkNextLogValue(STOPPED);
    }

    @Test
    void testStopFromUnloadingSuccessful() {
        conveyorTurnTable.load();
        await().until(() -> conveyorTurnTable.getConveyorStateMachine().isInState(FULLY_OCCUPIED));
        conveyorTurnTable.unload();
        conveyorTurnTable.stop();
        await().until(() -> conveyorTurnTable.getConveyorStateMachine().isInState(STOPPED));

        checkNextLogValue(LOADING);
        checkNextLogValue(FULLY_OCCUPIED);
        checkNextLogValue(UNLOADING);
        checkNextLogValue(STOPPING);
        checkNextLogValue(STOPPED);

    }

    @Test
    void testStopFromFullyOccupiedSuccessful() {
        conveyorTurnTable.load();
        await().until(() -> conveyorTurnTable.getConveyorStateMachine().isInState(FULLY_OCCUPIED));
        conveyorTurnTable.stop();
        await().until(() -> conveyorTurnTable.getConveyorStateMachine().isInState(STOPPED));

        checkNextLogValue(LOADING);
        checkNextLogValue(FULLY_OCCUPIED);
        checkNextLogValue(STOPPING);
        checkNextLogValue(STOPPED);
    }

    @Test
    void testStopFromSuspendedSuccessful() {
        conveyorTurnTable.load();
        conveyorTurnTable.pause();
        conveyorTurnTable.stop();
        await().until(() -> conveyorTurnTable.getConveyorStateMachine().isInState(STOPPED));

        checkNextLogValue(LOADING);
        checkNextLogValue(SUSPENDED);
        checkNextLogValue(STOPPING);
        checkNextLogValue(STOPPED);
    }

    @Test
    void testStopFromResetSuccessful() {
        conveyorTurnTable.stop();
        await().until(() -> conveyorTurnTable.getConveyorStateMachine().isInState(STOPPED));
        conveyorTurnTable.reset();
        conveyorTurnTable.stop();
        await().until(() -> conveyorTurnTable.getConveyorStateMachine().isInState(STOPPED));

        checkNextLogValue(STOPPING);
        checkNextLogValue(STOPPED);
        checkNextLogValue(RESETTING);
        checkNextLogValue(STOPPING);
        checkNextLogValue(STOPPED);
    }
    /*
     * ========================================= SM Checks ======================================== *
     */

    void checkLogValue(ConveyorStates expected) {
        ConveyorTurnTable.ConveyorProperties properties = conveyorTurnTable.getLogHistory().get(historyIndex);
        assertEquals(expected, properties.getConveyorState());
        switch (properties.getConveyorState()) {
            case IDLE:
                checkValidIdleState(properties);
                break;
            case LOADING:
                checkValidLoadingState(properties);
                break;
            case FULLY_OCCUPIED:
                checkValidFullyOccupiedState(properties);
                break;
            case UNLOADING:
                checkValidUnloadingState(properties);
                break;
            case SUSPENDED:
                checkValidSuspendedState(properties);
                break;
            case RESETTING:
                checkValidResetState(properties);
                break;
            case STOPPING:
                checkValidStoppingState(properties);
                break;
            case STOPPED:
                checkValidStoppedState(properties);
                break;
        }
    }

    void checkNextLogValue(ConveyorStates expected) {
        historyIndex++;
        checkLogValue(expected);
    }

    void checkValidIdleState(ConveyorTurnTable.ConveyorProperties conveyorProperties) {
        assertEquals(IDLE, conveyorProperties.getConveyorState());
        assertFalse(conveyorProperties.isMotorIsRunning());
        assertFalse(conveyorProperties.isLoadingSensorHasInput());
        assertFalse(conveyorProperties.isUnloadingSensorHasInput());
    }

    void checkValidLoadingState(ConveyorTurnTable.ConveyorProperties conveyorProperties) {
        assertEquals(ConveyorStates.LOADING, conveyorProperties.getConveyorState());
        assertTrue(conveyorProperties.isMotorIsRunning());
        assertFalse(conveyorProperties.isLoadingSensorHasInput());
        assertTrue(conveyorProperties.isUnloadingSensorHasInput());
    }

    void checkValidUnloadingState(ConveyorTurnTable.ConveyorProperties conveyorProperties) {
        assertEquals(ConveyorStates.UNLOADING, conveyorProperties.getConveyorState());
        assertTrue(conveyorProperties.isMotorIsRunning());
        assertFalse(conveyorProperties.isLoadingSensorHasInput());
    }

    void checkValidSuspendedState(ConveyorTurnTable.ConveyorProperties conveyorProperties) {
        assertEquals(ConveyorStates.SUSPENDED, conveyorProperties.getConveyorState());
        assertFalse(conveyorProperties.isMotorIsRunning());
    }

    void checkValidResetState(ConveyorTurnTable.ConveyorProperties conveyorProperties) {
        assertEquals(ConveyorStates.RESETTING, conveyorProperties.getConveyorState());
        assertFalse(conveyorProperties.isMotorIsRunning());
    }

    void checkValidFullyOccupiedState(ConveyorTurnTable.ConveyorProperties conveyorProperties) {
        assertEquals(ConveyorStates.FULLY_OCCUPIED, conveyorProperties.getConveyorState());
        assertFalse(conveyorProperties.isMotorIsRunning());
        assertTrue(conveyorProperties.isLoadingSensorHasInput());
        assertTrue(conveyorProperties.isUnloadingSensorHasInput());
    }

    void checkValidStoppingState(ConveyorTurnTable.ConveyorProperties conveyorProperties) {
        assertEquals(ConveyorStates.STOPPING, conveyorProperties.getConveyorState());
        assertFalse(conveyorProperties.isMotorIsRunning());
    }

    void checkValidStoppedState(ConveyorTurnTable.ConveyorProperties conveyorProperties) {
        assertEquals(STOPPED, conveyorProperties.getConveyorState());
        assertFalse(conveyorProperties.isMotorIsRunning());
    }


}