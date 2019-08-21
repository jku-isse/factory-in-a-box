package functionalUnitTurnTable;

import ev3dev.actuators.lego.motors.EV3LargeRegulatedMotor;
import ev3dev.sensors.ev3.EV3TouchSensor;
import functionalUnitBase.TurningBase;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.Port;
import lejos.hardware.port.SensorPort;
import open62Wrap.SWIGTYPE_p_UA_Server;
import open62Wrap.ServerAPIBase;
import open62Wrap.UA_NodeId;
import open62Wrap.open62541;
import turnTable.TurnTableOrientation;
import uaMethods.turningMethods.ResetTurningMethod;
import uaMethods.turningMethods.StopTurningMethod;
import uaMethods.turningMethods.TurnToMethod;

public class TurningTurnTable extends TurningBase {
    private final EV3LargeRegulatedMotor turnMotor;
    private final EV3TouchSensor touchSensor;

    private UA_NodeId statusNodeId;

    private int rotationToNext;
    private TurnTableOrientation orientation;

    public TurningTurnTable(Port motorPort, Port sensorPort, int rotationToNext) {
        this.turnMotor = new EV3LargeRegulatedMotor(motorPort);
        this.touchSensor = new EV3TouchSensor(sensorPort);
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
        while (!touchSensor.isPressed()) {

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
        int b = open62541.UA_ACCESSLEVELMASK_WRITE | open62541.UA_ACCESSLEVELMASK_READ;
        statusNodeId = getServerAPIBase().addVariableNode(getServer(), turningFolder, open62541.UA_NODEID_NUMERIC(1, 57),
                "TurningStatus", open62541.UA_TYPES_INT32, b);
        new ResetTurningMethod(this).addMethod(server, serverAPIBase, turningFolder);
        new StopTurningMethod(this).addMethod(server, serverAPIBase, turningFolder);
        new TurnToMethod(this).addMethod(server, serverAPIBase, turningFolder);
    }
}
