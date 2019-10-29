package hardware.actuators;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This is a Mock implementation of a Motor. It can be used for testing, although it does not account for
 * errors in the construction.
 */
public class MockMotor extends Motor {

    private AtomicBoolean running;
    private ScheduledExecutorService executorService;
    private ScheduledFuture forwardTask;
    private ScheduledFuture backwardTask;
    private int motorSpeed;

    public MockMotor() {
        running = new AtomicBoolean(false);
        executorService = new ScheduledThreadPoolExecutor(1);
        motorSpeed = 0;         //To prevent
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void forward() {
        running.set(true);
        forwardTask = executorService.scheduleAtFixedRate(
                () -> System.out.println("===| Moving forward with speed: " + motorSpeed),
                0, 1000, TimeUnit.MILLISECONDS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void backward() {
        running.set(true);
        backwardTask = executorService.scheduleAtFixedRate(
                () -> System.out.println("===| Moving backward with speed: " + motorSpeed),
                0, 1000, TimeUnit.MILLISECONDS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if(forwardTask != null){
            forwardTask.cancel(true);
        }
        if(backwardTask != null){
            backwardTask.cancel(true);
        }
        System.out.println("Motor stopped");
        running.set(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSpeed(int speed) {
        motorSpeed = speed;
        System.out.println("Set speed to " + speed);
    }

    public int getSpeed(){
        return motorSpeed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void waitMs(long period) {
        if (period <= 0) return;
        long end = System.currentTimeMillis() + period;
        boolean interrupted = false;
        do {
            try {
                Thread.sleep(period);
            } catch (InterruptedException ie) {
                interrupted = true;
            }
            period = end - System.currentTimeMillis();
        } while (period > 0);
        if (interrupted)
            Thread.currentThread().interrupt();
    }

    /**
     * Determines if the motor is running
     *
     * @return if motor is running
     */
    public boolean isRunning() {
        return running.get();
    }
}
