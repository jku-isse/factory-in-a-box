package hardware.sensors;

import ev3dev.hardware.EV3DevMotorDevice;
import lejos.hardware.port.SensorPort;

public abstract class Sensor {

    public abstract boolean detectedInput();       //TODO find better name

}
