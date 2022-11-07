package hardware.mock;

import hardware.TurningHardware;
import hardware.actuators.TurningMockMotor;
import hardware.sensors.MockSensor;

public class TurningMockHardware extends TurningHardware {

    /**
     * Constructs a default implementation of a mock instance
     */
    public TurningMockHardware() {
        //sensorHoming = new MockSensor();
        //turningMotor = new TurningMockMotor((MockSensor) sensorHoming, 100);
        this(100);
    }

    /**
     * Constructs a mock hardware instance
     * @param speed speed of the turning motor
     */
    public TurningMockHardware(int speed) {
        sensorHoming = new MockSensor();
        turningMotor = new TurningMockMotor((MockSensor) sensorHoming, speed);
    }

}
