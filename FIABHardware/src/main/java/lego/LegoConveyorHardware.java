package lego;

import hardware.ConveyorHardware;
import actuators.Motor;
import sensors.Sensor;

public class LegoConveyorHardware extends ConveyorHardware {

    public LegoConveyorHardware(Motor conveyorMotor, Sensor loadingSensor, Sensor unloadingSensor) {
        this.conveyorMotor = conveyorMotor;
        this.loadingSensor = loadingSensor;
        this.unloadingSensor = unloadingSensor;
    }
}
