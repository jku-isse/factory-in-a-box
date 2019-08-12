package functionalUnitTurnTable;

import ev3dev.actuators.lego.motors.EV3MediumRegulatedMotor;
import ev3dev.sensors.ev3.EV3TouchSensor;
import functionalUnitBase.ConveyorBase;
import open62Wrap.SWIGTYPE_p_UA_Server;
import open62Wrap.ServerAPIBase;
import open62Wrap.UA_NodeId;

public class ConveyorTurnTable extends ConveyorBase {

    private final EV3MediumRegulatedMotor mediumRegulatedMotor;
    private final EV3TouchSensor touchSensor;

    public ConveyorTurnTable(EV3MediumRegulatedMotor mediumRegulatedMotor, EV3TouchSensor touchSensor) {
        this.mediumRegulatedMotor = mediumRegulatedMotor;
        this.touchSensor = touchSensor;
    }

    @Override
    public void load() {
        System.out.println("Executing: loadBelt");
        this.mediumRegulatedMotor.brake();
        this.mediumRegulatedMotor.forward();
        while(!touchSensor.isPressed()){
            //TODO replace empty while loop with something not as ugly
        }
        this.mediumRegulatedMotor.stop();
    }

    @Override
    public void unload() {
        System.out.println("Executing: loadBelt");
        this.mediumRegulatedMotor.brake();
        this.mediumRegulatedMotor.backward();
        while(!touchSensor.isPressed()){
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

    }
}
