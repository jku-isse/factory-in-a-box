package lego;

import actuators.Motor;
import hardware.InputStationHardware;
import sensors.Sensor;

public class LegoInputStationHardware extends InputStationHardware {

    public LegoInputStationHardware(Motor releaseMotor, Sensor palletSensor) {
        this.releaseMotor = releaseMotor;
        this.palletSensor = palletSensor;
    }
}
