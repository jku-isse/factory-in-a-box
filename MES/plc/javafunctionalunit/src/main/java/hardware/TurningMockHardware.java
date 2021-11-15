package hardware;

import hardware.actuators.TurningMockMotor;
import hardware.sensors.MockSensor;
import lombok.Getter;

public class TurningMockHardware {

    @Getter private TurningMockMotor turningMockMotor;
    @Getter private MockSensor mockSensorHoming;

    public TurningMockHardware(int speed) {
        mockSensorHoming = new MockSensor();
        turningMockMotor = new TurningMockMotor(mockSensorHoming, speed);
    }
}
