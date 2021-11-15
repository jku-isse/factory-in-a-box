package hardware;

import hardware.actuators.Motor;
import hardware.sensors.Sensor;

public abstract class ConveyorHardware {

    protected Motor conveyorMotor;
    protected Sensor loadingSensor;
    protected Sensor unloadingSensor;

    public Motor getConveyorMotor() {
        return conveyorMotor;
    }

    public Sensor getLoadingSensor() {
        return loadingSensor;
    }

    public Sensor getUnloadingSensor() {
        return unloadingSensor;
    }
}
