package functionalUnitTurnTable;

import com.github.oxo42.stateless4j.StateMachine;
import functionalUnitBase.ConveyorBase;
import hardware.motors.Motor;
import hardware.sensors.Sensor;
import io.vertx.core.Vertx;
import open62Wrap.SWIGTYPE_p_UA_Server;
import open62Wrap.ServerAPIBase;
import open62Wrap.UA_NodeId;
import open62Wrap.open62541;
import stateMachines.conveyor.ConveyorStateMachineConfig;
import stateMachines.conveyor.ConveyorStates;
import stateMachines.conveyor.ConveyorTriggers;
import uaMethods.conveyorMethods.*;

import static stateMachines.conveyor.ConveyorStates.*;
import static stateMachines.conveyor.ConveyorTriggers.*;

/**
 * TurnTable implementation of the ConveyorBase. It has a stateMachine that tracks the status and guarantees stability.
 * One Motor is used for the conveyor belt and two sensors check whether the package has entered or left the belt.
 */
public class ConveyorTurnTable extends ConveyorBase {

    private final Motor conveyorMotor;
    private final Sensor sensorLoading;
    private final Sensor sensorUnloading;

    private final StateMachine<ConveyorStates, ConveyorTriggers> conveyorStateMachine;

    private UA_NodeId statusNodeId;
    private boolean stopped, suspended;

    public ConveyorTurnTable(Motor conveyorMotor, Sensor sensorLoading, Sensor sensorUnloading) {
        this.conveyorMotor = conveyorMotor;
        this.sensorLoading = sensorLoading;
        this.sensorUnloading = sensorUnloading;
        this.conveyorStateMachine = new StateMachine<>(STOPPED, new ConveyorStateMachineConfig());
        Runtime.getRuntime().addShutdownHook(new Thread(this.conveyorMotor::stop));
        this.stopped = false;
    }

    private void updateState() {
        getServerAPIBase().writeVariable(getServer(), statusNodeId, conveyorStateMachine.getState().getValue());
    }

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

    @Override
    public void addServerConfig(SWIGTYPE_p_UA_Server server, ServerAPIBase serverAPIBase, UA_NodeId conveyorFolder) {
        int b = open62541.UA_ACCESSLEVELMASK_WRITE | open62541.UA_ACCESSLEVELMASK_READ;
        statusNodeId = getServerAPIBase().addVariableNode(getServer(), conveyorFolder, open62541.UA_NODEID_NUMERIC(1, 56),
                "ConveyorStatus", open62541.UA_TYPES_INT32, b);
        new LoadMethod(this).addMethod(server, serverAPIBase, conveyorFolder);
        new UnloadMethod(this).addMethod(server, serverAPIBase, conveyorFolder);
        new PauseMethod(this).addMethod(server, serverAPIBase, conveyorFolder);
        new ResetConveyorMethod(this).addMethod(server, serverAPIBase, conveyorFolder);
        new StopConveyorMethod(this).addMethod(server, serverAPIBase, conveyorFolder);
        updateState();
    }
}
