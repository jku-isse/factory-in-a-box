package functionalUnitTurnTable;

import com.github.oxo42.stateless4j.StateMachine;
import ev3dev.actuators.lego.motors.EV3LargeRegulatedMotor;
import ev3dev.sensors.ev3.EV3TouchSensor;
import functionalUnitBase.TurningBase;
import io.vertx.core.Vertx;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.Port;
import lejos.hardware.port.SensorPort;
import lejos.utility.Delay;
import open62Wrap.SWIGTYPE_p_UA_Server;
import open62Wrap.ServerAPIBase;
import open62Wrap.UA_NodeId;
import open62Wrap.open62541;
import stateMachines.turning.TurningStateMachineConfig;
import stateMachines.turning.TurningStates;
import stateMachines.turning.TurningTriggers;
import turnTable.TurnTableOrientation;
import uaMethods.turningMethods.ResetTurningMethod;
import uaMethods.turningMethods.StopTurningMethod;
import uaMethods.turningMethods.TurnToMethod;

import static stateMachines.turning.TurningStates.*;
import static stateMachines.turning.TurningTriggers.*;

public class TurningTurnTable extends TurningBase {
    /**
     * TODO fix reset as it does not work properly anymore :(
     */
    private final EV3LargeRegulatedMotor turnMotor;
    private final EV3TouchSensor touchSensor;

    private UA_NodeId statusNodeId;

    private TurnTableOrientation orientation;
    private final StateMachine<TurningStates, TurningTriggers> turningStateMachine;
    private boolean stopped;

    public TurningTurnTable(Port motorPort, Port sensorPort, int rotationToNext) {
        this.turnMotor = new EV3LargeRegulatedMotor(motorPort);
        turnMotor.setSpeed(200);
        this.touchSensor = new EV3TouchSensor(sensorPort);
        turningStateMachine = new StateMachine<>(STOPPED, new TurningStateMachineConfig());
        Runtime.getRuntime().addShutdownHook(new Thread(turnMotor::stop));
        stopped = false;
    }

    private void updateState() {
        getServerAPIBase().writeVariable(getServer(), statusNodeId, turningStateMachine.getState().getValue());
    }

    /**
     * Turns left by the amount of degrees specified in rotationToNext
     */
    private void turnLeft() {
        System.out.println("Executing: turnLeft");
        this.turnMotor.brake();
        this.turnMotor.backward();
        Delay.msDelay(1400);
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
        this.turnMotor.brake();
        //this.turnMotor.rotate(rotationToNext);
        this.turnMotor.forward();
        Delay.msDelay(1400);
        this.turnMotor.stop();
        this.orientation = orientation.getNextClockwise(orientation);
        System.out.println("Orientation is now: " + orientation);
    }

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
                    turnMotor.brake();
                    turnMotor.backward();
                    while (!touchSensor.isPressed()) {
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

    @Override
    public void addServerConfig(SWIGTYPE_p_UA_Server server, ServerAPIBase serverAPIBase, UA_NodeId turningFolder) {
        int b = open62541.UA_ACCESSLEVELMASK_WRITE | open62541.UA_ACCESSLEVELMASK_READ;
        statusNodeId = getServerAPIBase().addVariableNode(getServer(), turningFolder, open62541.UA_NODEID_NUMERIC(1, 57),
                "TurningStatus", open62541.UA_TYPES_INT32, b);
        new ResetTurningMethod(this).addMethod(server, serverAPIBase, turningFolder);
        new StopTurningMethod(this).addMethod(server, serverAPIBase, turningFolder);
        new TurnToMethod(this).addMethod(server, serverAPIBase, turningFolder);
        updateState();
    }
}
