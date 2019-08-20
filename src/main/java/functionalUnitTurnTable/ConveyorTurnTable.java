package functionalUnitTurnTable;

import com.github.oxo42.stateless4j.StateMachine;
import ev3dev.actuators.lego.motors.EV3MediumRegulatedMotor;
import ev3dev.sensors.ev3.EV3TouchSensor;
import functionalUnitBase.ConveyorBase;
import io.vertx.core.Vertx;
import lejos.hardware.port.Port;
import lejos.utility.Delay;
import open62Wrap.SWIGTYPE_p_UA_Server;
import open62Wrap.ServerAPIBase;
import open62Wrap.UA_NodeId;
import open62Wrap.open62541;
import turnTable.TurnTableStateMachineConfig;
import turnTable.TurnTableStates;
import turnTable.TurnTableTriggers;
import uaMethods.conveyorMethods.LoadMethod;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import static turnTable.TurnTableStates.IDLE;
import static turnTable.TurnTableTriggers.NEXT;
import static turnTable.TurnTableTriggers.START;

public class ConveyorTurnTable extends ConveyorBase {

    private final EV3MediumRegulatedMotor mediumRegulatedMotor;
    private final EV3TouchSensor touchSensor;
    private final StateMachine<TurnTableStates, TurnTableTriggers> conveyorStateMachine;
    private UA_NodeId node;

    public ConveyorTurnTable(Port motorPort, Port sensorPort) {
        this.mediumRegulatedMotor = new EV3MediumRegulatedMotor(motorPort);
        this.touchSensor = new EV3TouchSensor(sensorPort);
        this.conveyorStateMachine = new StateMachine<>(IDLE, new TurnTableStateMachineConfig());
        Runtime.getRuntime().addShutdownHook(new Thread(mediumRegulatedMotor::stop));
    }

    private void updateState() {
        getServerAPIBase().writeVariable(getServer(), node, conveyorStateMachine.getState().getValue());
    }

    @Override
    public void load() {
        Vertx vertx = Vertx.vertx();    //If defined for entire class the program will terminate
        vertx.executeBlocking(promise -> {
            if (!conveyorStateMachine.canFire(START)) {
                System.out.println("Conveyor is busy");
                return;
            }
            conveyorStateMachine.fire(START);
            updateState();
            System.out.println("Executing: loadBelt");
            this.mediumRegulatedMotor.brake();
            this.mediumRegulatedMotor.backward();
            while (!touchSensor.isPressed()) {
                //TODO replace empty while loop with something not as ugly
            }
            System.out.println("Button pressed");
            this.mediumRegulatedMotor.stop();
            if (conveyorStateMachine.canFire(NEXT)) {
                conveyorStateMachine.fire(NEXT);
                updateState();
            }
        }, res -> {
        });
    }

    @Override
    public void unload() {
        Vertx vertx = Vertx.vertx();    //If defined for entire class the program will terminate
        vertx.executeBlocking(promise -> {
            if (!conveyorStateMachine.canFire(START)) {
                System.out.println("Conveyor is busy");
                return;
            }
            conveyorStateMachine.fire(START);
            updateState();
            System.out.println("Executing: loadBelt");
            this.mediumRegulatedMotor.brake();
            this.mediumRegulatedMotor.forward();
            while (!touchSensor.isPressed()) {
                //TODO replace empty while loop with something not as ugly
            }
            System.out.println("Button pressed");
            this.mediumRegulatedMotor.stop();
            if (conveyorStateMachine.canFire(NEXT)) {
                conveyorStateMachine.fire(NEXT);
                updateState();
            }
        }, res -> {
        });
    }

    @Override
    public void pause() {
        System.out.println("Executing: pause");
        this.mediumRegulatedMotor.brake();
        this.mediumRegulatedMotor.hold();
    }

    @Override
    public void reset() {
        System.out.println("Executing: reset");
        this.mediumRegulatedMotor.brake();
        this.mediumRegulatedMotor.rotateTo(0);
    }

    @Override
    public void stop() {
        System.out.println("Executing: stop");
        this.mediumRegulatedMotor.stop();
    }

    @Override
    public void addServerConfig(SWIGTYPE_p_UA_Server server, ServerAPIBase serverAPIBase, UA_NodeId conveyorFolder) {
        new LoadMethod(this).addMethod(server, serverAPIBase, conveyorFolder);
        int b = open62541.UA_ACCESSLEVELMASK_WRITE | open62541.UA_ACCESSLEVELMASK_READ;
        node = getServerAPIBase().addVariableNode(getServer(), conveyorFolder, 56,
                "ConveyorStatus", open62541.UA_TYPES_INT32, b);
    }
}
