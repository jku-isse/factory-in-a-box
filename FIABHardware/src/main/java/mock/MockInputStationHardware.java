package mock;

import actuators.Motor;
import hardware.InputStationHardware;
import sensors.Sensor;

public class MockInputStationHardware extends InputStationHardware {

    public MockInputStationHardware(Motor releaseMotor, Sensor palletSensor) {
        this.releaseMotor = releaseMotor;
        this.palletSensor = palletSensor;
    }
}
