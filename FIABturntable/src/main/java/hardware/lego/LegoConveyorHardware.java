package hardware.lego;

import hardware.ConveyorHardware;
import hardware.actuators.Motor;
import hardware.actuators.motorsEV3.MediumMotorEV3;
import hardware.sensors.Sensor;
import hardware.sensors.sensorsEV3.ColorSensorEV3;
import hardware.sensors.sensorsEV3.TouchSensorEV3;
import lejos.hardware.port.Port;

public class LegoConveyorHardware extends ConveyorHardware {

    public LegoConveyorHardware(Motor conveyorMotor, Sensor loadingSensor, Sensor unloadingSensor) {
        this.conveyorMotor = conveyorMotor;
        this.loadingSensor = loadingSensor;
        this.unloadingSensor = unloadingSensor;
    }
}
