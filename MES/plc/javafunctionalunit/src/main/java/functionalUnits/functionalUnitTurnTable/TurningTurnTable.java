package functionalUnits.functionalUnitTurnTable;

import com.github.oxo42.stateless4j.StateMachine;
import communication.utils.RequestedNodePair;
import functionalUnits.TurningBase;
import hardware.actuators.Motor;
import hardware.sensors.Sensor;
import io.vertx.core.Vertx;
import robot.turnTable.TurnTableOrientation;
import stateMachines.turning.TurningStateMachineConfig;

import static stateMachines.turning.TurningStates.STOPPED;
import static stateMachines.turning.TurningTriggers.*;

/**
 * TurnTable implementation for the Turning FU.
 * It uses one turnMotor and a sensor for homing.
 * The state machine tracks the state and guarantees stability
 */
public class TurningTurnTable extends TurningBase {

    private final Motor turnMotor;
    private final Sensor resetSensor;

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
        this.turnMotor.setSpeed(200);
        this.resetSensor = resetSensor;
        this.turningStateMachine = new StateMachine<>(STOPPED, new TurningStateMachineConfig());
        Runtime.getRuntime().addShutdownHook(new Thread(turnMotor::stop));
        stopped = false;
    }

    /**
     * Updates the current state on the server
     */
    private void updateState() {
        if(getServerCommunication() != null) {
            getServerCommunication().writeVariable(getServer(), statusNodeId, turningStateMachine.getState().getValue());
        }
    }

    /**
     * Turns left by the amount of degrees specified in rotationToNext
     */
    private void turnLeft() {
        System.out.println("Executing: turnLeft");
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
        System.out.println("Executing: turnRight");
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
        System.out.println("Executing: turnTo " + target);
        Vertx vertx = Vertx.vertx();
        vertx.executeBlocking(promise -> {
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
            turningStateMachine.fire(NEXT);
            updateState();
            //Find out what to do here (completing -> complete)
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
                    turningStateMachine.fire(RESET);
                    updateState();
                    System.out.println("Executing: reset");
                    turnMotor.backward();
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
            stop();
            return "Stopping Successful";
        });
        addStringMethodToServer(new RequestedNodePair<>(1, 33), "TurnToMethod", x -> {
            if (x.matches("^[0-3]$")) {
                turnTo(TurnTableOrientation.createFromInt(Integer.parseInt(x)));
                return "Turning to " + TurnTableOrientation.createFromInt(Integer.parseInt(x)) + " Successful";
            }
            return "Invalid input";
        });
        updateState();
    }
}
