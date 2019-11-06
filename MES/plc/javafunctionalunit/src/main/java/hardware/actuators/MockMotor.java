package hardware.actuators;

import hardware.sensors.MockSensor;
import lombok.Getter;
import lombok.Setter;

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

    @Getter @Setter private ScheduledExecutorService executorService;
    @Getter @Setter private ScheduledFuture forwardTask;
    @Getter @Setter private ScheduledFuture backwardTask;
    @Getter @Setter private int motorSpeed;
    @Getter @Setter private AtomicBoolean running;
    @Setter private MockSensor sensorLoading;
    @Setter private MockSensor sensorUnloading;

    public MockMotor(int speed) {
        running = new AtomicBoolean(false);
        executorService = new ScheduledThreadPoolExecutor(1);
        motorSpeed = speed;
    }

    @Override
    public void forward() {
        running.set(true);
        //setLoadingSensorInput(true);
        forwardTask = executorService.scheduleAtFixedRate(
                () -> System.out.println("===| Moving forward with speed: " + motorSpeed),
                0, 1000, TimeUnit.MILLISECONDS);

    }

    @Override
    public void backward() {
        running.set(true);
        //setUnloadingSensorInput(true);
        backwardTask = executorService.scheduleAtFixedRate(
                () -> System.out.println("===| Moving backward with speed: " + motorSpeed),
                0, 1000, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        running.set(false);
        if (forwardTask != null) {
            forwardTask.cancel(true);
        }
        if (backwardTask != null) {
            backwardTask.cancel(true);
        }
        System.out.println("Motor stopped");
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

    public boolean isRunning() {
        return running.get();
    }

    private void setLoadingSensorInput(boolean input){
        if(sensorLoading == null){
            return;
        }
        sensorLoading.setDetectedInput(input);
    }


    private void setUnloadingSensorInput(boolean input){
        if(sensorUnloading == null){
            return;
        }
        sensorLoading.setDetectedInput(input);
    }
}
