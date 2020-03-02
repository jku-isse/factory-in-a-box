package hardware.sensors.sensorsEV3;

import ev3dev.sensors.ev3.EV3TouchSensor;
import hardware.sensors.Sensor;
import lejos.hardware.port.Port;

public class TouchSensorEV3 extends Sensor {

    private final EV3TouchSensor touchSensor;

    public TouchSensorEV3(Port sensorPort) {
        touchSensor = new EV3TouchSensor(sensorPort);
    }

    @Override
    public boolean hasDetectedInput() {
        return this.touchSensor.isPressed();
    }
}
