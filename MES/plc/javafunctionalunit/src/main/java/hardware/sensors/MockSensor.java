package hardware.sensors;

import lombok.Getter;
import lombok.Setter;

/**
 * This is a Mock implementation of a Sensor. It can be used for testing, although it does not account for
 * errors in the construction.
 */
public class MockSensor extends Sensor {

    @Getter @Setter private boolean detectedInput;

    public MockSensor(){
        detectedInput = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDetectedInput() {
        return detectedInput;
    }
}
