package functionalUnits.functionalUnitTurnTable;

import com.github.oxo42.stateless4j.StateMachine;
import communication.utils.RequestedNodePair;
import functionalUnits.ConveyorBase;
import hardware.actuators.Motor;
import hardware.sensors.Sensor;
import io.vertx.core.Vertx;
import stateMachines.conveyor.ConveyorStateMachineConfig;
import stateMachines.conveyor.ConveyorStates;
import stateMachines.conveyor.ConveyorTriggers;

import static stateMachines.conveyor.ConveyorStates.STOPPED;
import static stateMachines.conveyor.ConveyorTriggers.*;
import static stateMachines.conveyor.ConveyorTriggers.LOAD;
import static stateMachines.conveyor.ConveyorTriggers.NEXT;
import static stateMachines.conveyor.ConveyorTriggers.PAUSE;
import static stateMachines.conveyor.ConveyorTriggers.RESET;
import static stateMachines.conveyor.ConveyorTriggers.STOP;
import static stateMachines.conveyor.ConveyorTriggers.UNLOAD;

/**
 * TurnTable implementation of the ConveyorBase. It has a stateMachine that tracks the status and guarantees stability.
 * One Motor is used for the conveyor belt and two sensors check whether the package has entered or left the belt.
 */
public class ConveyorTurnTable extends ConveyorBase {

    private final Motor conveyorMotor;
    private final Sensor sensorLoading;
    private final Sensor sensorUnloading;

    private final StateMachine<ConveyorStates, ConveyorTriggers> conveyorStateMachine;

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

    /**
     * Updates the state on the server
     */
    private void updateState() {
        getServerCommunication().writeVariable(getServer(), statusNodeId, conveyorStateMachine.getState().getValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        Vertx vertx = Vertx.vertx();    //If defined for entire class the program will terminate
        vertx.executeBlocking(promise -> {
            if (!conveyorStateMachine.canFire(LOAD)) {
                System.out.println("Conveyor is busy");
                return;
            }
            conveyorStateMachine.fire(LOAD);
            updateState();
            System.out.println("Executing: loadBelt");
            this.conveyorMotor.backward();
            while (!sensorLoading.detectedInput()) {
                if (stopped || suspended) {
                    stopped = false;
                    suspended = false;
                    vertx.close();
                    return;
                }
            }
            System.out.println("Button pressed");
            this.conveyorMotor.stop();
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
        Vertx vertx = Vertx.vertx();    //If defined for entire class the program will terminate
        vertx.executeBlocking(promise -> {
            if (!conveyorStateMachine.canFire(UNLOAD)) {
                System.out.println("Conveyor is busy");
                return;
            }
            conveyorStateMachine.fire(UNLOAD);
            updateState();
            System.out.println("Executing: loadBelt");
            this.conveyorMotor.forward();
            while (sensorUnloading.detectedInput()) {
                if (stopped || suspended) {
                    stopped = false;
                    suspended = false;
                    vertx.close();
                    return;
                }
            }
            System.out.println("No color detected");
            this.conveyorMotor.stop();
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
            suspended = true;
            conveyorStateMachine.fire(PAUSE);
            updateState();
            System.out.println("Executing: pause");
            this.conveyorMotor.stop();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        if (conveyorStateMachine.canFire(RESET)) {
            System.out.println("Executing: reset");
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
            conveyorStateMachine.fire(NEXT);
            //}
            updateState();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (!conveyorStateMachine.canFire(STOP)) {
            return;
        }
        conveyorStateMachine.fire(STOP);
        updateState();
        System.out.println("Executing: stop");
        stopped = true;
        this.conveyorMotor.stop();
        conveyorStateMachine.fire(NEXT);
        updateState();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addServerConfig() {
        statusNodeId = getServerCommunication().addIntegerVariableNode(getServer(), getObject(), new RequestedNodePair<>(1, 56),
                "ConveyorStatus");
        getServerCommunication().addStringMethod(getServerCommunication(), getServer(), getObject(),
                new RequestedNodePair<>(1, 21), "LoadMethod", x -> {
                    load();
                    return "Loading Successful";
                });
        getServerCommunication().addStringMethod(getServerCommunication(), getServer(), getObject(),
                new RequestedNodePair<>(1, 22), "UnloadMethod", x -> {
                    unload();
                    return "Unloading Successful";
                });
        /*new LoadMethod(this).addMethod();
        new UnloadMethod(this).addMethod();
        new PauseMethod(this).addMethod();
        new ResetConveyorMethod(this).addMethod();
        new StopConveyorMethod(this).addMethod();*/
        updateState();
    }
}
