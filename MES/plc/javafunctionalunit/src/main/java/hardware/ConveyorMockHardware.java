package hardware;

import hardware.actuators.ConveyorMockMotor;
import hardware.sensors.MockSensor;
import lombok.Getter;

public class ConveyorMockHardware {

    @Getter private ConveyorMockMotor conveyorMockMotor = null;
    @Getter private MockSensor mockSensorLoading = null;
    @Getter private MockSensor mockSensorUnloading = null;

    public ConveyorMockHardware(int speed, long delay) {
        mockSensorLoading = new MockSensor();
        mockSensorUnloading = new MockSensor();
        conveyorMockMotor = new ConveyorMockMotor(mockSensorLoading, mockSensorUnloading, speed, delay);
    }
}
