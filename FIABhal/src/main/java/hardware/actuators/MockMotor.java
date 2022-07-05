package hardware.actuators;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * This is a Mock implementation of a Motor. It can be used for testing, although it does not account for
 * errors in the construction.
 */
public class MockMotor extends Motor {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private int motorSpeed;
    private int currentAngle;

    protected ScheduledExecutorService executorService;
    private ScheduledFuture<?> forwardTask;
    private ScheduledFuture<?> backwardTask;
    private ScheduledFuture<?> rotateTask;

    public MockMotor(int speed) {
        super();
        executorService = new ScheduledThreadPoolExecutor(1);
        motorSpeed = speed;
        currentAngle = 0;
    }

    public int getMotorSpeed() {
        return motorSpeed;
    }

    public void setMotorSpeed(int motorSpeed) {
        this.motorSpeed = motorSpeed;
    }

    @Override
    public void forward() {
        super.forward();
        forwardTask = executorService.scheduleAtFixedRate(
                () -> {
                    logger.info("===| Moving forward with speed: " + motorSpeed);
                    currentAngle++;
                },
                0, 1000, TimeUnit.MILLISECONDS);
    }

    @Override
    public void backward() {
        super.backward();
        backwardTask = executorService.scheduleAtFixedRate(
                () -> {
                    logger.info("===| Moving backward with speed: " + motorSpeed);
                    currentAngle--;
                },
                0, 1000, TimeUnit.MILLISECONDS);
    }

    @Override
    public void rotate(int angle) {
        rotateTask = executorService.scheduleAtFixedRate(
                () -> {
                    logger.info("===| Rotating to angle: " + angle +
                            " with speed " + motorSpeed + ". Current angle: " + currentAngle);
                    if (currentAngle < angle) {
                        //Arbitrary numbers used to simulate fast motor turning to position
                        currentAngle = (int) (currentAngle + Math.max((Math.abs(currentAngle - angle) * 0.8), 1));
                    } else if (currentAngle > angle) {
                        currentAngle = currentAngle - Math.max((Math.abs(currentAngle - angle) / 2), 1);
                    }
                },
                0, motorSpeed, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        super.stop();
        logger.info("Motor stopped");
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
    public int getRotationAngle() {
        return currentAngle;
    }

    @Override
    public void setSpeed(int speed) {
        motorSpeed = speed;
        logger.info("Set speed to " + speed);
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
        logger.info("Tacho Count Reset");
    }
}