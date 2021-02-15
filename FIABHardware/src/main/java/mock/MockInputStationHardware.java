package mock;

import actuators.InputStationMockMotor;
import actuators.Motor;
import hardware.InputStationHardware;
import sensors.MockSensor;
import sensors.Sensor;

public class MockInputStationHardware extends InputStationHardware {

    public MockInputStationHardware(Motor releaseMotor, Sensor palletSensor) {
        this.releaseMotor = releaseMotor;
        ((InputStationMockMotor) this.releaseMotor).setPalletSensor((MockSensor) palletSensor);
        this.palletSensor = ((InputStationMockMotor) releaseMotor).getPalletSensor();
    }
}
