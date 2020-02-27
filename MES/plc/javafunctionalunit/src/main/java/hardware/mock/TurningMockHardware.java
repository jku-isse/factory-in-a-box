package hardware.mock;

import hardware.TurningHardware;
import hardware.actuators.TurningMockMotor;
import hardware.sensors.MockSensor;

public class TurningMockHardware extends TurningHardware {

    public TurningMockHardware(int speed) {
        sensorHoming = new MockSensor();
        turningMotor = new TurningMockMotor((MockSensor) sensorHoming, speed);
    }

}
