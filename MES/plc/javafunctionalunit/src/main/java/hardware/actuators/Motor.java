package hardware.actuators;

/**
 * This class represents a Motor. Any motor can be used, provided it implements these methods.
 */
public abstract class Motor {

    private boolean isRunning;

    protected Motor(){
        isRunning = false;
    }
    /**
     * Spins the motor in forward direction.
     */
    public void forward(){
        isRunning = true;
    }

    /**
     * Spins the motor in backward direction.
     */
    public void backward(){
        isRunning = true;
    }

    /**
     * Stops the motor
     */
    public void stop(){
        isRunning = false;
    }

    /**
     * Sets the speed on the motor
     * @param speed value depends on motor
     */
    public abstract void setSpeed(int speed);

    /**
     * Waits x milliseconds. If motor library has a delay method, use it instead of thread.sleep
     * @param msDelay delay in ms
     */
    public abstract void waitMs(long msDelay);

    public boolean isRunning(){
        return isRunning;
    }
}
