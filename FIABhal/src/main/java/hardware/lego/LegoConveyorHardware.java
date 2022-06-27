package hardware.lego;

import hardware.ConveyorHardware;
import hardware.actuators.motorsEV3.MediumMotorEV3;
import hardware.sensors.sensorsEV3.ColorSensorEV3;
import hardware.sensors.sensorsEV3.TouchSensorEV3;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.Port;
import lejos.hardware.port.SensorPort;

public class LegoConveyorHardware extends ConveyorHardware {

    /**
     * Use this for the default configuration of the EV3 Turntable
     */
    public LegoConveyorHardware(){
        this(MotorPort.A, SensorPort.S2, SensorPort.S3);
    }

    /**
     * Use this when the configuration on the EV3 has changed
     * @param motorPort port where the motor is connected to
     * @param loadingSensorPort port where the loading sensor is connected
     * @param unloadingSensorPort port where the unloading sensor is connected
     */
    public LegoConveyorHardware(Port motorPort, Port loadingSensorPort, Port unloadingSensorPort){
        conveyorMotor = new MediumMotorEV3(motorPort);
        loadingSensor = new TouchSensorEV3(loadingSensorPort);
        unloadingSensor = new ColorSensorEV3(unloadingSensorPort);
    }
}
