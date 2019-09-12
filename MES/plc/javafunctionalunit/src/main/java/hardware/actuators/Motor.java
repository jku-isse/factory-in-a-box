package hardware.actuators;

public abstract class Motor {

    public abstract void forward();

    public abstract void backward();

    public abstract void stop();

    public abstract void setSpeed(int speed);

    public abstract void waitMs(int msDelay);
}
