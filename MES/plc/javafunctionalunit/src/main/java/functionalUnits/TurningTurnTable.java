package functionalUnits;

import com.github.oxo42.stateless4j.StateMachine;
import communication.utils.Pair;
import functionalUnits.base.TurningBase;
import hardware.actuators.Motor;
import hardware.sensors.Sensor;
import io.vertx.core.Vertx;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import stateMachines.turning.TurnTableOrientation;
import stateMachines.turning.TurningStateMachineConfig;
import stateMachines.turning.TurningStates;

import java.util.ArrayList;
import java.util.List;

import static stateMachines.turning.TurningStates.STOPPED;
import static stateMachines.turning.TurningTriggers.*;

/**
 * TurnTable implementation for the Turning FU.
 * It uses one turnMotor and a sensor for homing.
 * The state machine tracks the state and guarantees stability
 */
public class TurningTurnTable extends TurningBase {

    @AllArgsConstructor
    @ToString
    @EqualsAndHashCode
    class TurningProperties {
        @Getter private TurningStates conveyorState;
        @Getter private boolean motorIsRunning, homingSensorHasInput;
    }

    private List<TurningProperties> logHistory;

    private final Motor turnMotor;
    private final Sensor resetSensor;

    @Getter private TurnTableOrientation orientation;
    private Object statusNodeId;
    private boolean stopped;

    /**
     * Turning FU for a TurnTable.
     *
     * @param turnMotor   motor to specify turning. If turning in wrong direction, exchange forward with backwards.
     * @param resetSensor motor used for homing
     */
    public TurningTurnTable(Motor turnMotor, Sensor resetSensor) {
        this.turnMotor = turnMotor;
        this.turnMotor.setSpeed(200);
        this.resetSensor = resetSensor;
        this.turningStateMachine = new StateMachine<>(STOPPED, new TurningStateMachineConfig());
        Runtime.getRuntime().addShutdownHook(new Thread(turnMotor::stop));
        stopped = false;
    }

    public List<TurningProperties> getLogHistory() {
        return logHistory != null ? logHistory : new ArrayList<>();
    }

    /**
     * Updates the current state on the server
     */
    private void updateState() {
        if (getServerCommunication() != null) {
            getServerCommunication().writeVariable(getServer(), statusNodeId, turningStateMachine.getState().getValue());
        } else {
            if (logHistory == null) {
                logHistory = new ArrayList<>();
            }
            logHistory.add(new TurningProperties(turningStateMachine.getState(), turnMotor.isRunning(),
                    resetSensor.hasDetectedInput()));
        }
    }

    /**
     * Turns left by the amount of degrees specified in rotationToNext
     */
    private void turnLeft() {
        System.out.println("Executing from turning: turnLeft");
        turnMotor.backward();
        turnMotor.waitMs(1400);
        turnMotor.stop();
        //this.turnMotor.rotate(-rotationToNext);
        orientation = orientation.getNextCounterClockwise(orientation);
        System.out.println("Orientation is now: " + orientation);
    }

    /**
     * Turns right by the amount of degrees specified in rotationToNext
     */
    private void turnRight() {
        System.out.println("Executing from turning: turnRight");
        turnMotor.forward();
        turnMotor.waitMs(1400);
        turnMotor.stop();
        orientation = orientation.getNextClockwise(orientation);
        System.out.println("Orientation is now: " + orientation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void turnTo(TurnTableOrientation target) {
        if (!turningStateMachine.canFire(TURN_TO)) {
            return;
        }
        turningStateMachine.fire(TURN_TO);
        updateState();
        System.out.println("Executing from turning: turnTo " + target);
        Vertx vertx = Vertx.vertx();
        vertx.executeBlocking(promise -> {
            turningStateMachine.fire(EXECUTE);
            updateState();
            if (target.getNumericValue() > this.orientation.getNumericValue()) {
                while (!(target.getNumericValue() == this.orientation.getNumericValue())) {
                    if (stopped) {
                        turningStateMachine.fire(STOP);
                        updateState();
                        stopped = false;
                        vertx.close();
                        return;
                    }
                    turnRight();
                }
            } else {
                while (!(target.getNumericValue() == this.orientation.getNumericValue())) {
                    if (stopped) {
                        turningStateMachine.fire(STOP);
                        updateState();
                        stopped = false;
                        vertx.close();
                        return;
                    }
                    turnLeft();
                }
            }
            turningStateMachine.fire(NEXT);
            updateState();
            //Find out what to do here (completing -> complete -> idle)
            turningStateMachine.fire(NEXT);
            updateState();
            turningStateMachine.fire(NEXT);
            updateState();
        }, res -> {
        });
        vertx.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        if (!turningStateMachine.canFire(RESET)) {
            return;
        }
        Vertx vertx = Vertx.vertx();
        vertx.executeBlocking(promise -> {
                    stopped = false;
                    System.out.println("Executing from turning: reset");
                    turnMotor.backward();
                    turningStateMachine.fire(RESET);
                    updateState();
                    while (!resetSensor.hasDetectedInput()) {
                        if (stopped) {
                            stopped = false;
                            turningStateMachine.fire(STOP);
                            vertx.close();
                            return;
                        }
                    }
                    turnMotor.stop();
                    turningStateMachine.fire(NEXT);
                    updateState();
                    orientation = TurnTableOrientation.NORTH;
                },
                res -> {
                });
        vertx.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        turningStateMachine.fire(STOP);
        updateState();
        System.out.println("Executing from turning: stop");
        stopped = true;
        turnMotor.stop();
        turningStateMachine.fire(NEXT);
        updateState();
    }

    public enum TurningStringIdentifiers {
        STATE, RESET, STOP, TURN_TO
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addServerConfig() {
        final String PREFIX = "TURNING_";
        statusNodeId = getServerCommunication().addIntegerVariableNode(getServer(), getObject(),
                new Pair<>(1, PREFIX + TurningStringIdentifiers.STATE.name()),
                PREFIX + TurningStringIdentifiers.STATE.name());
        getServerCommunication().addStringMethod(getServerCommunication(), getServer(), getObject(),
                new Pair<>(1, PREFIX + TurningStringIdentifiers.RESET.name()),
                PREFIX + TurningStringIdentifiers.RESET.name(), input -> {
                    reset();
                    return "Turning: Resetting Successful";
                });
        getServerCommunication().addStringMethod(getServerCommunication(), getServer(), getObject(),
                new Pair<>(1, PREFIX + TurningStringIdentifiers.STOP.name()),
                PREFIX + TurningStringIdentifiers.STOP.name(), input -> {
                    stop();
                    return "Turning: Stopping Successful";
                });
        getServerCommunication().addStringMethod(getServerCommunication(), getServer(), getObject(),
                new Pair<>(1, PREFIX + TurningStringIdentifiers.TURN_TO.name()),
                PREFIX + TurningStringIdentifiers.TURN_TO.name(), input -> {
                    if (input.matches("^[0-3]$")) {
                        turnTo(TurnTableOrientation.createFromInt(Integer.parseInt(input)));
                        return "Turning to " + TurnTableOrientation.createFromInt(Integer.parseInt(input)) + " Successful";
                    }
                    return "Turning: Invalid input";
                });
        updateState();
    }
}
