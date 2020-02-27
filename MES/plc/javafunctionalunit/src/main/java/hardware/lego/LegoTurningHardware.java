package hardware.lego;

import hardware.TurningHardware;
import hardware.actuators.motorsEV3.LargeMotorEV3;
import hardware.sensors.sensorsEV3.TouchSensorEV3;
import lejos.hardware.port.Port;

public class LegoTurningHardware extends TurningHardware {

    public LegoTurningHardware(Port motorPort, Port sensorPort){
        turningMotor = new LargeMotorEV3(motorPort);
        sensorHoming = new TouchSensorEV3(sensorPort);
    }
}
