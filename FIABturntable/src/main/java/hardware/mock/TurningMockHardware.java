package hardware.mock;

import hardware.TurningHardware;
import hardware.actuators.Motor;
import hardware.actuators.TurningMockMotor;
import hardware.sensors.MockSensor;
import hardware.sensors.Sensor;

public class TurningMockHardware extends TurningHardware {

    public TurningMockHardware(Motor turningMotor, Sensor sensorHoming) {
        this.sensorHoming = sensorHoming;
        this.turningMotor = turningMotor;
        ((TurningMockMotor) this.turningMotor).setSensorHoming((MockSensor) this.sensorHoming);
    }

}
