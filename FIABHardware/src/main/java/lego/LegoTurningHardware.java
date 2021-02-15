package lego;

import hardware.TurningHardware;
import actuators.Motor;
import sensors.Sensor;

public class LegoTurningHardware extends TurningHardware {

    public LegoTurningHardware(Motor turningMotor, Sensor sensorHoming) {
        this.turningMotor = turningMotor;
        this.sensorHoming = sensorHoming;
    }
}
