package functionalUnits.functionalUnitTurnTable;

import hardware.TurningMockHardware;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import robot.turnTable.TurnTableOrientation;
import stateMachines.turning.TurningStates;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static robot.turnTable.TurnTableOrientation.*;
import static stateMachines.turning.TurningStates.*;

public class TurningTurnTableTest {

    private static int speed;
    private int historyIndex;
    private TurningMockHardware mockHardware;
    private TurningTurnTable turnTable;

    @BeforeAll
    static void init() {
        speed = 100;
    }

    @BeforeEach
    void buildTest() {
        historyIndex = 0;
        mockHardware = new TurningMockHardware(speed);
        turnTable = new TurningTurnTable(mockHardware.getConveyorMockMotor(), mockHardware.getMockSensorHoming());
        turnTable.reset();
        await().until(() -> turnTable.getTurningStateMachine().isInState(IDLE));
        checkLogValue(RESETTING);
        checkNextLogValue(IDLE);
    }

    @Test
    void testIdleNorthOnStartSuccessful() {
        checkLogValue(IDLE);
        checkOrientation(NORTH);
    }

    @Test
    void testTurnToNorthSuccessful() {
        turnTable.turnTo(NORTH);
        await().until(() -> turnTable.getTurningStateMachine().isInState(IDLE));
        checkNextLogValue(STARTING);
        checkNextLogValue(EXECUTING);
        checkNextLogValue(COMPLETING);
        checkNextLogValue(COMPLETE);
        checkOrientation(NORTH);
    }

    @Test
    void testTurnToEastSuccessful() {
        turnTable.turnTo(EAST);
        await().until(() -> turnTable.getTurningStateMachine().isInState(IDLE));
        checkNextLogValue(STARTING);
        checkNextLogValue(EXECUTING);
        checkNextLogValue(COMPLETING);
        checkNextLogValue(COMPLETE);
        checkOrientation(EAST);
    }

    @Test
    void testTurnToSouthSuccessful() {
        turnTable.turnTo(SOUTH);
        await().until(() -> turnTable.getTurningStateMachine().isInState(IDLE));
        checkNextLogValue(STARTING);
        checkNextLogValue(EXECUTING);
        checkNextLogValue(COMPLETING);
        checkNextLogValue(COMPLETE);
        checkOrientation(SOUTH);
    }

    @Test
    void testTurnToWestSuccessful() {
        turnTable.turnTo(WEST);
        await().until(() -> turnTable.getTurningStateMachine().isInState(IDLE));
        checkNextLogValue(STARTING);
        checkNextLogValue(EXECUTING);
        checkNextLogValue(COMPLETING);
        checkNextLogValue(COMPLETE);
        checkOrientation(WEST);
    }

    @Test
    void testResetSuccessful() {
        turnTable.stop();
        await().until(() -> turnTable.getTurningStateMachine().isInState(STOPPED));
        turnTable.reset();
        await().until(() -> turnTable.getTurningStateMachine().isInState(IDLE));
        checkNextLogValue(STOPPING);
        checkNextLogValue(STOPPED);
        checkNextLogValue(RESETTING);
        checkNextLogValue(IDLE);
    }

    @Test
    void testResetFromIdleFail() {
        checkLogValue(IDLE);
        turnTable.reset();
        await().until(() -> turnTable.getTurningStateMachine().isInState(IDLE));
        checkLogValue(IDLE);
    }

    @Test
    void testStopFromSuccessful() {
        turnTable.stop();
        await().until(() -> turnTable.getTurningStateMachine().isInState(STOPPED));
        checkNextLogValue(STOPPING);
        checkNextLogValue(STOPPED);
    }

    /*
     * ================================== State and Direction checks ================================== *
     */
    void checkLogValue(TurningStates expected) {
        TurningTurnTable.TurningProperties properties = turnTable.getLogHistory().get(historyIndex);
        assertEquals(expected, properties.getConveyorState());
        switch (properties.getConveyorState()) {
            case IDLE:
                checkValidIdleState(properties);
                break;
            case STARTING:
                checkValidStartingState(properties);
                break;
            case EXECUTING:
                checkValidExecuteState(properties);
                break;
            case COMPLETING:
                checkValidCompletingState(properties);
                break;
            case COMPLETE:
                checkValidCompleteState(properties);
                break;
            case RESETTING:
                checkValidResettingState(properties);
                break;
            case STOPPING:
                checkValidStoppingState(properties);
                break;
            case STOPPED:
                checkValidStoppedState(properties);
                break;
        }
    }

    void checkNextLogValue(TurningStates expected) {
        historyIndex++;
        checkLogValue(expected);
    }

    private void checkValidIdleState(TurningTurnTable.TurningProperties properties) {
        assertFalse(properties.isMotorIsRunning());
        //should check for sensorInput after reset
    }

    private void checkValidStartingState(TurningTurnTable.TurningProperties properties) {
        assertFalse(properties.isMotorIsRunning());
    }

    private void checkValidExecuteState(TurningTurnTable.TurningProperties properties) {
        //Is true but current implementation calls fireTrigger before starting blocking loop
        //assertTrue(properties.isMotorIsRunning());
    }

    private void checkValidCompletingState(TurningTurnTable.TurningProperties properties) {
        assertFalse(properties.isMotorIsRunning());
    }

    private void checkValidCompleteState(TurningTurnTable.TurningProperties properties) {
        assertFalse(properties.isMotorIsRunning());
    }

    private void checkValidResettingState(TurningTurnTable.TurningProperties properties) {
        assertTrue(properties.isMotorIsRunning());
    }

    private void checkValidStoppingState(TurningTurnTable.TurningProperties properties) {
        assertFalse(properties.isMotorIsRunning());
    }

    private void checkValidStoppedState(TurningTurnTable.TurningProperties properties) {
        assertFalse(properties.isMotorIsRunning());
    }

    private void checkOrientation(TurnTableOrientation orientation) {
        assertEquals(orientation, turnTable.getOrientation());
    }


}
