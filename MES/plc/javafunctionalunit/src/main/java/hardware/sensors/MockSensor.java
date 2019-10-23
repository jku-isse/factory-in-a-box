package hardware.sensors;

import java.util.Random;

/**
 * This is a Mock implementation of a Sensor. It can be used for testing, although it does not account for
 * errors in the construction.
 */
public class MockSensor extends Sensor {

    private Random random;

    public MockSensor(){
        random = new Random();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean detectedInput() {
        return random.nextBoolean();
    }
}
