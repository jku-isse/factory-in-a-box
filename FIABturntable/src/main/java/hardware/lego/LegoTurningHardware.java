package hardware.lego;

import hardware.TurningHardware;
import hardware.actuators.Motor;
import hardware.actuators.motorsEV3.LargeMotorEV3;
import hardware.sensors.Sensor;
import hardware.sensors.sensorsEV3.TouchSensorEV3;
import lejos.hardware.port.Port;

public class LegoTurningHardware extends TurningHardware {

    public LegoTurningHardware(Motor turningMotor, Sensor sensorHoming) {
        this.turningMotor = turningMotor;
        this.sensorHoming = sensorHoming;
    }
}
