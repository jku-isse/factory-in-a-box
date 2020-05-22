package hardware.sensors;

/**
 * This is a Mock implementation of a Sensor. It can be used for testing, although it does not account for
 * errors in the construction.
 */
public class MockSensor extends Sensor {

    private boolean detectedInput;

    public MockSensor(){
        detectedInput = false;
    }

    public void setDetectedInput(boolean detectedInput) {
        this.detectedInput = detectedInput;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDetectedInput() {
        return detectedInput;
    }

}
