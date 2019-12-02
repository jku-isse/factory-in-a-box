package functionalUnits;

import com.github.oxo42.stateless4j.StateMachine;
import communication.utils.Pair;
import functionalUnits.base.ConveyorBase;
import hardware.actuators.Motor;
import hardware.sensors.Sensor;
import io.vertx.core.Vertx;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import stateMachines.conveyor.ConveyorStateMachineConfig;
import stateMachines.conveyor.ConveyorStates;

import java.util.ArrayList;
import java.util.List;

import static stateMachines.conveyor.ConveyorStates.STOPPED;
import static stateMachines.conveyor.ConveyorTriggers.*;

/**
 * TurnTable implementation of the ConveyorBase. It has a stateMachine that tracks the status and guarantees stability.
 * One Motor is used for the conveyor belt and two sensors check whether the package has entered or left the belt.
 */
public class ConveyorTurnTable extends ConveyorBase {

    @AllArgsConstructor
    @ToString
    @EqualsAndHashCode
    class ConveyorProperties {
        @Getter private ConveyorStates conveyorState;
        @Getter private boolean motorIsRunning, loadingSensorHasInput, unloadingSensorHasInput;
    }

    private final Motor conveyorMotor;
    private final Sensor sensorLoading;
    private final Sensor sensorUnloading;

    private List<ConveyorProperties> logHistory;

    private Object statusNodeId;
    private boolean stopped, suspended;

    /**
     * Creates a new Conveyor FU for a TurnTable.
     *
     * @param conveyorMotor   conveyor motor used. If turning in the wrong direction replace forward with backward
     * @param sensorLoading   sensor to check if the pallet is loaded
     * @param sensorUnloading sensor to check if pallet is unloaded
     */
    public ConveyorTurnTable(Motor conveyorMotor, Sensor sensorLoading, Sensor sensorUnloading) {
        this.conveyorMotor = conveyorMotor;
        this.sensorLoading = sensorLoading;
        this.sensorUnloading = sensorUnloading;
        this.conveyorStateMachine = new StateMachine<>(STOPPED, new ConveyorStateMachineConfig());
        Runtime.getRuntime().addShutdownHook(new Thread(this.conveyorMotor::stop));
        this.stopped = false;
    }

    public List<ConveyorProperties> getLogHistory() {
        return logHistory != null ? logHistory : new ArrayList<>();
    }

    /**
     * Updates the state on the server
     */
    private void updateState() {
        if (getServerCommunication() != null) {
            getServerCommunication().writeVariable(getServer(), statusNodeId, conveyorStateMachine.getState().getValue());
        } else {
            if (logHistory == null) {
                logHistory = new ArrayList<>();
            }
            logHistory.add(new ConveyorProperties(conveyorStateMachine.getState(), conveyorMotor.isRunning(),
                    sensorLoading.hasDetectedInput(), sensorUnloading.hasDetectedInput()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        if (!conveyorStateMachine.canFire(LOAD)) {
            System.out.println("Conveyor is busy");
            return;
        }
        System.out.println("Executing from conveyor: loadBelt");
        conveyorMotor.backward();
        conveyorStateMachine.fire(LOAD);
        updateState();
        Vertx vertx = Vertx.vertx();    //If defined for entire class the program will terminate
        vertx.executeBlocking(promise -> {
            while (!sensorLoading.hasDetectedInput()) {
                //  System.out.print(".");
                if (stopped || suspended) {
                    stopped = false;
                    suspended = false;
                    vertx.close();
                    return;
                }
            }
            System.out.println("Button pressed");
            conveyorMotor.stop();
            conveyorStateMachine.fire(NEXT);
            updateState();
        }, res -> {
        });
        vertx.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unload() {
        if (!conveyorStateMachine.canFire(UNLOAD)) {
            System.out.println("Conveyor is busy");
            return;
        }
        System.out.println("Executing from conveyor: loadBelt");
        conveyorMotor.forward();
        conveyorStateMachine.fire(UNLOAD);
        updateState();
        Vertx vertx = Vertx.vertx();    //If defined for entire class the program will terminate
        vertx.executeBlocking(promise -> {
            while (sensorUnloading.hasDetectedInput()) {
                if (stopped || suspended) {
                    stopped = false;
                    suspended = false;
                    vertx.close();
                    return;
                }
            }
            System.out.println("No color detected");
            conveyorMotor.stop();
            conveyorStateMachine.fire(NEXT);
            updateState();
        }, res -> {
        });
        vertx.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void pause() {
        if (conveyorStateMachine.canFire(PAUSE)) {
            System.out.println("Executing from conveyor: pause");
            suspended = true;
            this.conveyorMotor.stop();
            conveyorStateMachine.fire(PAUSE);
            updateState();
        } else {
            System.out.println("Cannot pause from " + getConveyorStateMachine().getState());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        System.out.println("Trying to reset conveyor");
        if (conveyorStateMachine.canFire(RESET)) {
            System.out.println("Executing from conveyor: reset");
            conveyorStateMachine.fire(RESET);
            updateState();
            this.conveyorMotor.stop();
            stopped = false;
            suspended = false;/*
            if(touchSensor.isPressed()){
                conveyorStateMachine.fire(NEXT_FULL);
            }else if(colorSensor.getColorID() != Color.NONE){
                conveyorStateMachine.fire(NEXT_PARTIAL);
            }else{*/
            Vertx vertx = Vertx.vertx();
            vertx.executeBlocking(promise -> {
                conveyorStateMachine.fire(NEXT);
                updateState();
            }, res -> {
            });
            //}
        } else {
            System.out.println("Cannot reset from: " + getConveyorStateMachine().getState());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        conveyorMotor.stop();
        conveyorStateMachine.fire(STOP);
        updateState();
        System.out.println("Executing: stop");
        stopped = true;
        Vertx vertx = Vertx.vertx();
        vertx.executeBlocking(promise -> {
                    conveyorStateMachine.fire(NEXT);
                    updateState();
                },
                res -> {
                });
    }

    public enum ConveyorStringIdentifiers {
        STATE, LOAD, UNLOAD, PAUSE, RESET, STOP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addServerConfig() {
        final String PREFIX = "CONVEYOR_";  //Use StringBuilder
        statusNodeId = getServerCommunication().addStringVariableNode(getServer(), getObject(),
                new Pair<>(1, PREFIX + ConveyorStringIdentifiers.STATE.name()), PREFIX + ConveyorStringIdentifiers.STATE.name());
        getServerCommunication().addStringMethod(getServerCommunication(), getServer(), getObject(),
                new Pair<>(1, PREFIX + ConveyorStringIdentifiers.LOAD.name()), PREFIX + ConveyorStringIdentifiers.LOAD.name(), input -> {
                    load();
                    return "Conveyor: Loading Successful";
                });
        getServerCommunication().addStringMethod(getServerCommunication(), getServer(), getObject(),
                new Pair<>(1, PREFIX + ConveyorStringIdentifiers.UNLOAD.name()), PREFIX + ConveyorStringIdentifiers.UNLOAD.name(), input -> {
                    unload();
                    return "Conveyor: Unloading Successful";
                });
        getServerCommunication().addStringMethod(getServerCommunication(), getServer(), getObject(),
                new Pair<>(1, PREFIX + ConveyorStringIdentifiers.PAUSE.name()), PREFIX + ConveyorStringIdentifiers.PAUSE.name(), input -> {
                    pause();
                    return "Conveyor: Pausing Successful";
                });
        getServerCommunication().addStringMethod(getServerCommunication(), getServer(), getObject(),
                new Pair<>(1, PREFIX + ConveyorStringIdentifiers.RESET.name()), PREFIX + ConveyorStringIdentifiers.RESET.name(), input -> {
                    reset();
                    return "Conveyor: Resetting Successful";
                });
        getServerCommunication().addStringMethod(getServerCommunication(), getServer(), getObject(),
                new Pair<>(1, PREFIX + ConveyorStringIdentifiers.STOP.name()), PREFIX + ConveyorStringIdentifiers.STOP.name(), input -> {
                    stop();
                    return "Conveyor: Stop Successful";
                });
        updateState();
    }
}
