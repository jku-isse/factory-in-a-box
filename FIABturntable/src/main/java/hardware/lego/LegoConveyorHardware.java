package hardware.lego;

import hardware.ConveyorHardware;
import hardware.actuators.motorsEV3.MediumMotorEV3;
import hardware.sensors.sensorsEV3.ColorSensorEV3;
import hardware.sensors.sensorsEV3.TouchSensorEV3;
import lejos.hardware.port.Port;

public class LegoConveyorHardware extends ConveyorHardware {

    public LegoConveyorHardware(Port motorPort, Port loadingSensorPort, Port unloadingSensorPort){
        conveyorMotor = new MediumMotorEV3(motorPort);
        loadingSensor = new TouchSensorEV3(loadingSensorPort);
        unloadingSensor = new ColorSensorEV3(unloadingSensorPort);
    }
}
