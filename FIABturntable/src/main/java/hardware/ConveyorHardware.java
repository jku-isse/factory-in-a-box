package hardware;

import hardware.actuators.Motor;
import hardware.sensors.Sensor;
import lombok.Getter;

public abstract class ConveyorHardware {

    @Getter protected Motor conveyorMotor;
    @Getter protected Sensor loadingSensor;
    @Getter protected Sensor unloadingSensor;


}
