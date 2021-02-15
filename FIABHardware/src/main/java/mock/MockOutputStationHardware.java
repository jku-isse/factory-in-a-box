package mock;

import hardware.OutputStationHardware;
import sensors.Sensor;

public class MockOutputStationHardware extends OutputStationHardware {

    public MockOutputStationHardware(Sensor palletSensor) {
        this.palletSensor = palletSensor;
    }
}
