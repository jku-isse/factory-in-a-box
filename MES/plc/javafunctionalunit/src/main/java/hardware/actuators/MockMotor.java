package hardware.actuators;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This is a Mock implementation of a Motor. It can be used for testing, although it does not account for
 * errors in the construction.
 */
public class MockMotor extends Motor {

    private AtomicBoolean running;

    public MockMotor() {
        running = new AtomicBoolean(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void forward() {
        new Thread(() -> {
            running.set(true);
            while (isRunning()) {
                try {
                    System.out.println("Moving forward ...");
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void backward() {
        new Thread(() -> {
            running.set(true);
            while (isRunning()) {
                try {
                    System.out.println("Moving backward ...");
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        System.out.println("Motor stopped");
        running.set(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSpeed(int speed) {
        System.out.println("Set speed to " + speed);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void waitMs(int msDelay) {
        new Thread(() -> {
            try {
                Thread.sleep(msDelay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Determines if the motor is running
     * @return if motor is running
     */
    private boolean isRunning() {
        return running.get();
    }
}
