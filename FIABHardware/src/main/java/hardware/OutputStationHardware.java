package hardware;

import sensors.Sensor;

public abstract class OutputStationHardware {

    protected Sensor palletSensor;

    public Sensor getPalletSensor() {
        return palletSensor;
    }
}
