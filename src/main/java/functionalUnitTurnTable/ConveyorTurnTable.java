package functionalUnitTurnTable;

import com.github.oxo42.stateless4j.StateMachine;
import ev3dev.actuators.lego.motors.EV3MediumRegulatedMotor;
import ev3dev.sensors.ev3.EV3TouchSensor;
import functionalUnitBase.ConveyorBase;
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

    @Override
    public void load() {
        if (!conveyorStateMachine.canFire(START)) {
            System.out.println("Conveyor is busy");
            return;
        }
        conveyorStateMachine.fire(START);
        getServerAPIBase().writeVariable(getServer(), node, conveyorStateMachine.getState().getValue());
        System.out.println("Executing: loadBelt");
        CountDownLatch latch = new CountDownLatch(1);
        new Thread(() -> {
            this.mediumRegulatedMotor.brake();
            this.mediumRegulatedMotor.backward();
            while (!touchSensor.isPressed()) {
                //TODO replace empty while loop with something not as ugly
            }
            this.mediumRegulatedMotor.stop();
            latch.countDown();
        }).start();
        try {
            latch.await();
            if (conveyorStateMachine.canFire(NEXT)) {
                conveyorStateMachine.fire(NEXT);
                getServerAPIBase().writeVariable(getServer(), node, conveyorStateMachine.getState().getValue());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void unload() {
        System.out.println("Executing: loadBelt");
        this.mediumRegulatedMotor.brake();
        this.mediumRegulatedMotor.forward();
        while (!touchSensor.isPressed()) {
            //TODO replace empty while loop with something not as ugly
        }
        this.mediumRegulatedMotor.stop();
    }

    @Override
    public void pause() {
        this.mediumRegulatedMotor.brake();
        this.mediumRegulatedMotor.hold();
    }

    @Override
    public void reset() {
        this.mediumRegulatedMotor.brake();
        this.mediumRegulatedMotor.rotateTo(0);
    }

    @Override
    public void stop() {
        this.mediumRegulatedMotor.stop();
    }

    @Override
    public void addServerConfig(SWIGTYPE_p_UA_Server server, ServerAPIBase serverAPIBase, UA_NodeId conveyorFolder) {
        new LoadMethod(this).addMethod(server, serverAPIBase, conveyorFolder);
        int b = open62541.UA_ACCESSLEVELMASK_WRITE | open62541.UA_ACCESSLEVELMASK_READ;
        node = getServerAPIBase().addVariableNode(getServer(), conveyorFolder, 56, "Status", open62541.UA_TYPES_INT32, b);
    }
}
