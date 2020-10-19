package hardware.mock;

import hardware.ConveyorHardware;
import hardware.actuators.ConveyorMockMotor;
import hardware.actuators.MockMotor;
import hardware.actuators.Motor;
import hardware.sensors.MockSensor;
import hardware.sensors.Sensor;
import lombok.Getter;

public class ConveyorMockHardware extends ConveyorHardware {

//    @Getter private ConveyorMockMotor conveyorMockMotor = null;
//    @Getter private MockSensor mockSensorLoading = null;
//    @Getter private MockSensor mockSensorUnloading = null;

    public ConveyorMockHardware(Motor conveyorMotor, Sensor loadingSensor, Sensor unloadingSensor) {
        this.conveyorMotor = conveyorMotor;
        this.unloadingSensor = unloadingSensor;
        this.loadingSensor = loadingSensor;
        ((ConveyorMockMotor)this.conveyorMotor).setSensorLoading((MockSensor) this.loadingSensor);
        ((ConveyorMockMotor)this.conveyorMotor).setSensorUnloading((MockSensor) this.unloadingSensor);
    }
}
