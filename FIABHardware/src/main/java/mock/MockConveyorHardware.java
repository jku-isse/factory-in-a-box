package mock;

import hardware.ConveyorHardware;
import actuators.ConveyorMockMotor;
import actuators.Motor;
import sensors.MockSensor;
import sensors.Sensor;

public class MockConveyorHardware extends ConveyorHardware {

    public MockConveyorHardware(Motor conveyorMotor, Sensor loadingSensor, Sensor unloadingSensor) {
        this.conveyorMotor = conveyorMotor;
        this.unloadingSensor = unloadingSensor;
        this.loadingSensor = loadingSensor;
        ((ConveyorMockMotor)this.conveyorMotor).setSensorLoading((MockSensor) this.loadingSensor);
        ((ConveyorMockMotor)this.conveyorMotor).setSensorUnloading((MockSensor) this.unloadingSensor);
    }
}
