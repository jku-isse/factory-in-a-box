package hardware.mock;

import hardware.ConveyorHardware;
import hardware.actuators.ConveyorMockMotor;
import hardware.sensors.MockSensor;
import lombok.Getter;

public class ConveyorMockHardware extends ConveyorHardware {

//    @Getter private ConveyorMockMotor conveyorMockMotor = null;
//    @Getter private MockSensor mockSensorLoading = null;
//    @Getter private MockSensor mockSensorUnloading = null;

    public ConveyorMockHardware(int speed, long delay) {
        loadingSensor = new MockSensor();
        unloadingSensor = new MockSensor();
        conveyorMotor = new ConveyorMockMotor((MockSensor)loadingSensor, (MockSensor)unloadingSensor, speed, delay);
    	
    }
}
