package hardware.lego;

import hardware.TurningHardware;
import hardware.actuators.motorsEV3.LargeMotorEV3;
import hardware.sensors.sensorsEV3.TouchSensorEV3;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.Port;
import lejos.hardware.port.SensorPort;

public class LegoTurningHardware extends TurningHardware {

    /**
     * Creates a default instance of the Lego Turning hardware
     * This configuration is used as the default on the FIAB Turntable
     */
    public LegoTurningHardware() {
        turningMotor = new LargeMotorEV3(MotorPort.D);
        sensorHoming = new TouchSensorEV3(SensorPort.S4);
    }

    /**
     * Creates an instance of the Lego Turning hardware
     * @param motorPort port to which the turning motor is connected
     * @param sensorPort port to which the homing sensor is connected
     */
    LegoTurningHardware(Port motorPort, Port sensorPort){
        turningMotor = new LargeMotorEV3(motorPort);
        sensorHoming = new TouchSensorEV3(sensorPort);
    }
}
