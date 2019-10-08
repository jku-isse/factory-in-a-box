package hardware.actuators;

/**
 * This class represents a Motor. Any motor can be used, provided it implements these methods.
 */
public abstract class Motor {

    /**
     * Spins the motor in forward direction.
     */
    public abstract void forward();

    /**
     * Spins the motor in backward direction.
     */
    public abstract void backward();

    /**
     * Stops the motor
     */
    public abstract void stop();

    /**
     * Sets the speed on the motor
     * @param speed value depends on motor
     */
    public abstract void setSpeed(int speed);

    /**
     * Waits x milliseconds. If motor library has a delay method, use it instead of thread.sleep
     * @param msDelay delay in ms
     */
    public abstract void waitMs(int msDelay);
}
