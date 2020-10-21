package mock;

import hardware.TurningHardware;
import actuators.Motor;
import actuators.TurningMockMotor;
import sensors.MockSensor;
import sensors.Sensor;

public class MockTurningHardware extends TurningHardware {

    public MockTurningHardware(Motor turningMotor, Sensor sensorHoming) {
        this.sensorHoming = sensorHoming;
        this.turningMotor = turningMotor;
        ((TurningMockMotor) this.turningMotor).setSensorHoming((MockSensor) this.sensorHoming);
    }

}
