package hardware;

import actuators.Motor;
import sensors.Sensor;

public abstract class TurningHardware {
    protected Motor turningMotor;
    protected Sensor sensorHoming;

    public Motor getTurningMotor() {
        return turningMotor;
    }

    public Sensor getSensorHoming() {
        return sensorHoming;
    }
}
