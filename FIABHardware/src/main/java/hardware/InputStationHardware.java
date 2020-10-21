package hardware;

import actuators.Motor;
import sensors.Sensor;

public abstract class InputStationHardware {

    protected Motor releaseMotor;
    protected Sensor palletSensor;

    public Motor getReleaseMotor() {
        return releaseMotor;
    }

    public Sensor getPalletSensor() {
        return palletSensor;
    }
}
