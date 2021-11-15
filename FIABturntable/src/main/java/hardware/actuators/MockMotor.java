package hardware.actuators;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * This is a Mock implementation of a Motor. It can be used for testing, although it does not account for
 * errors in the construction.
 */
public class MockMotor extends Motor {

    @Getter @Setter private int motorSpeed;

    protected ScheduledExecutorService executorService;
    private ScheduledFuture forwardTask;
    private ScheduledFuture backwardTask;
    private ScheduledFuture rotateTask;

    public MockMotor(int speed) {
        super();
        executorService = new ScheduledThreadPoolExecutor(1);
        motorSpeed = speed;
    }

    @Override
    public void forward() {
        super.forward();
        forwardTask = executorService.scheduleAtFixedRate(
                () -> System.out.println("===| Moving forward with speed: " + motorSpeed),
                0, 1000, TimeUnit.MILLISECONDS);
    }

    @Override
    public void backward() {
        super.backward();
        backwardTask = executorService.scheduleAtFixedRate(
                () -> System.out.println("===| Moving backward with speed: " + motorSpeed),
                0, 1000, TimeUnit.MILLISECONDS);
    }

    //TODO fix for windows systems
    @Override
    public void rotate(int angle) {
        rotateTask = executorService.scheduleAtFixedRate(
                () -> {
                    System.out.println("===| Rotating: " + motorSpeed);
                },
                0, 1000, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        super.stop();
        System.out.println("Motor stopped");
        if (forwardTask != null) {
            forwardTask.cancel(true);
        }
        if (backwardTask != null) {
            backwardTask.cancel(true);
        }
        if (rotateTask != null) {
            rotateTask.cancel(true);
        }
    }

    @Override
    public void setSpeed(int speed) {
        motorSpeed = speed;
        System.out.println("Set speed to " + speed);
    }

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

    @Override
    public void resetTachoCount() {
        System.out.println("Tacho Count Reset");
    }
}
