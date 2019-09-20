package functionalUnits.functionalUnitTurnTable;

import com.github.oxo42.stateless4j.StateMachine;
import communication.utils.RequestedNodePair;
import functionalUnits.TurningBase;
import hardware.actuators.Motor;
import hardware.sensors.Sensor;
import io.vertx.core.Vertx;
import stateMachines.turning.TurningStateMachineConfig;
import stateMachines.turning.TurningStates;
import stateMachines.turning.TurningTriggers;
import robot.turnTable.TurnTableOrientation;


import java.util.function.Function;

import static stateMachines.turning.TurningStates.STOPPED;
import static stateMachines.turning.TurningTriggers.*;
import static stateMachines.turning.TurningTriggers.EXECUTE;
import static stateMachines.turning.TurningTriggers.NEXT;
import static stateMachines.turning.TurningTriggers.RESET;
import static stateMachines.turning.TurningTriggers.STOP;
import static stateMachines.turning.TurningTriggers.TURN_TO;

/**
 * TurnTable implementation for the Turning FU.
 * It uses one turnMotor and a sensor for homing.
 * The state machine tracks the state and guarantees stability
 */
public class TurningTurnTable extends TurningBase {

    private final Motor turnMotor;
    private final Sensor resetSensor;
    private final StateMachine<TurningStates, TurningTriggers> turningStateMachine;

    private TurnTableOrientation orientation;
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
        turnMotor.setSpeed(200);
        this.resetSensor = resetSensor;
        turningStateMachine = new StateMachine<>(STOPPED, new TurningStateMachineConfig());
        Runtime.getRuntime().addShutdownHook(new Thread(turnMotor::stop));
        stopped = false;
    }

    /**
     * Updates the current state on the server
     */
    private void updateState() {
        getServerCommunication().writeVariable(getServer(), statusNodeId, turningStateMachine.getState().getValue());
    }

    /**
     * Turns left by the amount of degrees specified in rotationToNext
     */
    private void turnLeft() {
        System.out.println("Executing: turnLeft");
        this.turnMotor.backward();
        this.turnMotor.waitMs(1400);
        this.turnMotor.stop();
        //this.turnMotor.rotate(-rotationToNext);
        this.orientation = orientation.getNextCounterClockwise(orientation);
        System.out.println("Orientation is now: " + orientation);
    }

    /**
     * Turns right by the amount of degrees specified in rotationToNext
     */
    private void turnRight() {
        System.out.println("Executing: turnRight");
        this.turnMotor.forward();
        this.turnMotor.waitMs(1400);
        this.turnMotor.stop();
        this.orientation = orientation.getNextClockwise(orientation);
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
        System.out.println("Executing: turnTo " +
                target);
        Vertx vertx = Vertx.vertx();
        vertx.executeBlocking(promise -> {
            System.out.println("Current State: " + turningStateMachine.getState());
            turningStateMachine.fire(TURN_TO);
            updateState();
            //Find out what to do here
            System.out.println("Current State: " + turningStateMachine.getState());
            turningStateMachine.fire(EXECUTE);
            updateState();
            if (target.getNumericValue() > this.orientation.getNumericValue()) {
                while (!(target.getNumericValue() == this.orientation.getNumericValue())) {
                    if (stopped) {
                        turningStateMachine.fire(STOP);
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
                        stopped = false;
                        vertx.close();
                        return;
                    }
                    turnLeft();
                }
            }
            System.out.println("Current State: " + turningStateMachine.getState());
            turningStateMachine.fire(NEXT);
            updateState();
            //Find out what to do here
            System.out.println("Current State: " + turningStateMachine.getState());
            turningStateMachine.fire(NEXT);
            updateState();
            System.out.println("Current State: " + turningStateMachine.getState());
            turningStateMachine.fire(NEXT);
            updateState();
            System.out.println("Finished in State: " + turningStateMachine.getState());
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
                    turningStateMachine.fire(RESET);
                    updateState();
                    System.out.println("Executing: reset");
                    turnMotor.backward();
                    while (!resetSensor.detectedInput()) {
                        if (stopped) {
                            stopped = false;
                            return;
                        }
                    }
                    turnMotor.stop();
                    turningStateMachine.fire(NEXT);
                    updateState();
                    this.orientation = TurnTableOrientation.NORTH;
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
        if (!turningStateMachine.canFire(STOP)) {
            return;
        }
        turningStateMachine.fire(STOP);
        updateState();
        System.out.println("Executing: stop");
        stopped = true;
        turnMotor.stop();
        turningStateMachine.fire(NEXT);
        updateState();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addServerConfig() {
        statusNodeId = getServerCommunication().addIntegerVariableNode(getServer(), getObject(),
                new RequestedNodePair<>(1, 57), "TurningStatus");
        addStringMethodToServer(new RequestedNodePair<>(1, 31), "ResetTurningMethod", x -> {
            reset();
            return "Resetting Successful";
        });
        addStringMethodToServer(new RequestedNodePair<>(1, 32), "StopTurningMethod", x -> {
            reset();
            return "Stopping Successful";
        });
        addStringMethodToServer(new RequestedNodePair<>(1, 33), "TurnToMethod", x -> {
            if(x.matches("^[0-3]$")){
                turnTo(TurnTableOrientation.createFromInt(Integer.parseInt(x)));
                return "Turning to " + TurnTableOrientation.createFromInt(Integer.parseInt(x)) + " Successful";
            }
            return "Invalid input";
        });
        updateState();
    }
}
