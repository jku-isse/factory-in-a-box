package hardware.actuators;

import hardware.sensors.Sensor;

/**
 * Mocks the Conveyor motor and sensor. Should have defined behaviour for the sensors.
 */
public class MockMotorConveyor extends Motor {

    private final Sensor loadingSensor;
    private final Sensor unloadingSensor;

    public MockMotorConveyor(Sensor loadingSensor, Sensor unloadingSensor){
        this.loadingSensor = loadingSensor;
        this.unloadingSensor = unloadingSensor;
    }

    @Override
    public void forward() {

    }

    @Override
    public void backward() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void setSpeed(int speed) {

    }

    @Override
    public void waitMs(int msDelay) {

    }
}
