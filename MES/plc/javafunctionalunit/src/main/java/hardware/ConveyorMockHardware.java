package hardware;

import hardware.actuators.ConveyorMockMotor;
import hardware.sensors.MockSensor;
import lombok.Getter;

public class ConveyorMockHardware {

    @Getter private ConveyorMockMotor conveyorMockMotor;
    @Getter private MockSensor mockSensorLoading;
    @Getter private MockSensor mockSensorUnloading;

    public ConveyorMockHardware(int speed, long delay){
        conveyorMockMotor = new ConveyorMockMotor(speed, delay);
        mockSensorLoading = new MockSensor();
        mockSensorUnloading = new MockSensor();

        conveyorMockMotor.setSensorLoading(mockSensorLoading);
        conveyorMockMotor.setSensorUnloading(mockSensorUnloading);
    }
}
