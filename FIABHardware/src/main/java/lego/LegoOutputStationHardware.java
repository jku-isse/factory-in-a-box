package lego;

import hardware.OutputStationHardware;
import sensors.Sensor;

public class LegoOutputStationHardware extends OutputStationHardware {

    public LegoOutputStationHardware(Sensor palletSensor) {
        this.palletSensor = palletSensor;
    }
}
