package hardware.sensors;

/**
 * This class represents a Sensor. Any sensor can be used, provided it implements these methods.
 */
public abstract class Sensor {

    /**
     * When the sensor is detecting an input it should return true, otherwise false.
     * @return if input was detected
     */
    public abstract boolean hasDetectedInput();

}
