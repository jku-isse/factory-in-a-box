package hardware.sensors.sensorsEV3;

import ev3dev.sensors.ev3.EV3ColorSensor;
import hardware.sensors.Sensor;
import lejos.hardware.port.Port;
import lejos.robotics.Color;

public class ColorSensorEV3 extends Sensor {

    private final EV3ColorSensor colorSensor;

    public ColorSensorEV3(Port sensorPort) {
        this.colorSensor = new EV3ColorSensor(sensorPort);
    }

    @Override
    public boolean hasDetectedInput() {
        return !(colorSensor.getColorID() == Color.NONE);
    }

}
