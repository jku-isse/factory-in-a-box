package hardware.sensors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a Mock implementation of a Sensor. It can be used for testing, although it does not account for
 * errors in the construction.
 */
public class MockSensor extends Sensor {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private boolean detectedInput;

    public MockSensor(){
        detectedInput = false;
    }

    public void setDetectedInput(boolean detectedInput) {
        this.detectedInput = detectedInput;
        if(detectedInput){
            logger.info("Sensor detected input");
        }else{
            logger.info("Sensor is no longer detecting input");
        }
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDetectedInput() {
        return detectedInput;
    }

}
