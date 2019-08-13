package functionalUnitTurnTable;

import ev3dev.actuators.lego.motors.EV3LargeRegulatedMotor;
import ev3dev.sensors.ev3.EV3TouchSensor;
import functionalUnitBase.TurningBase;
import open62Wrap.SWIGTYPE_p_UA_Server;
import open62Wrap.ServerAPIBase;
import open62Wrap.UA_NodeId;
import turnTable.TurnTableOrientation;

import java.util.HashMap;
import java.util.function.Function;

public class TurningTurnTable extends TurningBase {
    private final EV3LargeRegulatedMotor turnMotor;
    private final EV3TouchSensor touchSensor;

    private int rotationToNext;
    private TurnTableOrientation orientation;

    public TurningTurnTable(EV3LargeRegulatedMotor turnMotor, EV3TouchSensor touchSensor, int rotationToNext) {
        this.turnMotor = turnMotor;
        this.touchSensor = touchSensor;
        this.rotationToNext = rotationToNext;
        reset();
    }

    /**
     * Turns left by the amount of degrees specified in rotationToNext
     */
    private void turnLeft() {
        System.out.println("Executing: turnLeft");
        this.turnMotor.brake();
        this.turnMotor.rotate(rotationToNext);
        this.orientation = orientation.getNextCounterClockwise(orientation);
        System.out.println("Orientation is now: " + orientation);
    }

    /**
     * Turns right by the amount of degrees specified in rotationToNext
     */
    private void turnRight() {
        System.out.println("Executing: turnRight");
        this.turnMotor.brake();
        this.turnMotor.rotate(-rotationToNext);
        this.orientation = orientation.getNextClockwise(orientation);
        System.out.println("Orientation is now: " + orientation);
    }
    @Override
    public void turnTo(TurnTableOrientation target) {
        if (target.getNumericValue() > this.orientation.getNumericValue()) {
            while (!(target.getNumericValue() == this.orientation.getNumericValue())) {
                turnRight();
            }
        } else {
            while (!(target.getNumericValue() == this.orientation.getNumericValue())) {
                turnLeft();
            }
        }
    }

    @Override
    public void reset() {
        turnMotor.brake();
        turnMotor.backward();   //Test if this is the right direction
        while(!touchSensor.isPressed()){

        }
        turnMotor.stop();
        this.orientation = TurnTableOrientation.NORTH;
    }

    @Override
    public void stop() {
        turnMotor.stop();
    }

    @Override
    public void addServerConfig(SWIGTYPE_p_UA_Server server, ServerAPIBase serverAPIBase, UA_NodeId turningFolder) {

    }
}
