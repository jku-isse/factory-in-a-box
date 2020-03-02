package hardware;

import hardware.actuators.Motor;
import hardware.sensors.Sensor;
import lombok.Getter;

public abstract class TurningHardware {
    @Getter protected Motor turningMotor;
    @Getter protected Sensor sensorHoming;
}
