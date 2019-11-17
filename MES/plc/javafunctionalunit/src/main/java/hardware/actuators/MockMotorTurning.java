package hardware.actuators;

import hardware.sensors.Sensor;

/**
 * Mocks the Turning Motor and Sensor. Should have defined Sensor behaviour.
 */
public class MockMotorTurning extends Motor {

    private final Sensor sensor;

    public MockMotorTurning(){
        this.sensor = new Sensor() {
            @Override
            public boolean detectedInput() {
                return false;
            }
        };
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
