package functionalUnits.functionalUnitMock;

import com.github.oxo42.stateless4j.StateMachine;
import functionalUnits.TurningBase;
import hardware.actuators.MockMotor;
import hardware.sensors.MockSensor;
import lombok.Getter;
import robot.turnTable.TurnTableOrientation;
import stateMachines.turning.TurningStateMachineConfig;
import stateMachines.turning.TurningStates;
import stateMachines.turning.TurningTriggers;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static robot.turnTable.TurnTableOrientation.NORTH;
import static stateMachines.turning.TurningTriggers.*;

/**
 * This class should be used to simulate the behaviour of the turning funit.
 * The simulation has motors and sensors that interact with each other.
 * If you want to assign this to a funit, use the motors exposed though the getters.
 * Needs support for supporting error scenarios for testing.
 */
public class TurningMock extends TurningBase {

    private MockMotor turnMotor;
    private MockSensor sensorHoming;
    private ScheduledThreadPoolExecutor executor;
    @Getter private TurnTableOrientation currentOrientation;
    private int speed;
    @Getter private AtomicBoolean stopped;
    private ScheduledFuture resetTask;

    public TurningMock(int speed) {
        this.turnMotor = new MockMotor(speed);
        this.sensorHoming = new MockSensor();
        this.executor = new ScheduledThreadPoolExecutor(4);
        this.currentOrientation = NORTH;
        this.speed = speed;
        this.turningStateMachine = new StateMachine<>(TurningStates.STOPPED, new TurningStateMachineConfig());
        this.stopped = new AtomicBoolean(true);
    }

    @Override
    public void turnTo(TurnTableOrientation target) {
        if (!turningStateMachine.canFire(TurningTriggers.TURN_TO)) {
            return;
        }
        turningStateMachine.fire(TurningTriggers.TURN_TO);
        turningStateMachine.fire(EXECUTE);
        if (target.getNumericValue() > currentOrientation.getNumericValue()) {
            while (!(target.getNumericValue() == currentOrientation.getNumericValue())) {
                if (stopped.get()) {
                    turningStateMachine.fire(STOP);
                    stopped.set(false);
                    return;
                }
                turnRight();
            }
        } else {
            while (!(target.getNumericValue() == currentOrientation.getNumericValue())) {
                if (stopped.get()) {
                    turningStateMachine.fire(STOP);
                    stopped.set(false);
                    return;
                }
                turnLeft();
            }
        }
        turningStateMachine.fire(NEXT);
        turningStateMachine.fire(NEXT);
    }

    @Override
    public void reset() {
        if (!turningStateMachine.canFire(RESET)) {
            System.out.println("Could not reset, current state: " + turningStateMachine.getState());
            return;
        }
        turningStateMachine.fire(RESET);
        stopped.set(false);
        turnMotor.backward();
        resetTask = executor.schedule(() -> {
            sensorHoming.setDetectedInput(true);
            turnMotor.stop();
            turningStateMachine.fire(NEXT);
            currentOrientation = NORTH;
        }, 500, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        turningStateMachine.fire(STOP);
        stopped.set(true);
        resetTask.cancel(true);
        turnMotor.stop();
        turningStateMachine.fire(NEXT);
    }

    @Override
    public void addServerConfig() {
        System.out.println("Adding Server config");
    }

    public StateMachine<TurningStates, TurningTriggers> getTurningStateMachine() {
        return turningStateMachine;
    }

    /**
     * Turns left by the amount of degrees specified in rotationToNext
     */
    private void turnLeft() {
        System.out.println("Executing: turnLeft");
        turnMotor.backward();
        turnMotor.waitMs(100);
        turnMotor.stop();
        //this.turnMotor.rotate(-rotationToNext);
        currentOrientation = currentOrientation.getNextCounterClockwise(currentOrientation);
        if (currentOrientation == NORTH) {
            sensorHoming.setDetectedInput(true);
        }
        System.out.println("Orientation is now: " + currentOrientation);
    }

    /**
     * Turns right by the amount of degrees specified in rotationToNext
     */
    private void turnRight() {
        System.out.println("Executing: turnRight");
        turnMotor.forward();
        sensorHoming.setDetectedInput(false);
        turnMotor.waitMs(100);
        turnMotor.stop();
        currentOrientation = currentOrientation.getNextClockwise(currentOrientation);
        System.out.println("Orientation is now: " + currentOrientation);
    }
}
