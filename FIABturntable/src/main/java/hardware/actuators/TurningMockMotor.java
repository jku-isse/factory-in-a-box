package hardware.actuators;


import hardware.sensors.MockSensor;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TurningMockMotor extends MockMotor {

    private int rotationAngle;
    private MockSensor sensorHoming;
    private long delay;
    private boolean isTurningForward, isTurningBackward;
    private ScheduledThreadPoolExecutor executor;
    private ScheduledFuture timerTask;
    private ScheduledFuture rotationTask;

    public TurningMockMotor(MockSensor sensorHoming, int speed) {
        super(speed);
        this.rotationAngle = 0;
        this.sensorHoming = sensorHoming;
        this.delay = speed * 10L;     //simulate time for turning, assuming speed >= 100 (1s) and speed <= 500 (5s)
        isTurningForward = false;
        isTurningBackward = false;
        sensorHoming.setDetectedInput(false);
        executor = new ScheduledThreadPoolExecutor(2);
    }

    @Override
    public void forward() {
        super.forward();
        isTurningForward = true;
        isTurningBackward = false;
        this.rotationAngle = 10;    //For now we just use a non 0 value
    }

    @Override
    public void backward() {
        super.backward();
        isTurningForward = false;
        isTurningBackward = true;
        timerTask = executor.schedule(() -> {
                    sensorHoming.setDetectedInput(true);
                    this.rotationAngle = 0;
                },
                delay, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        super.stop();
        if (timerTask != null) {
            timerTask.cancel(true);
        }
        if (rotationTask != null) {
            rotationTask.cancel(true);
        }
        isTurningForward = false;
        isTurningBackward = false;
    }

    @Override
    public void rotateTo(int degree) {
        super.rotateTo(degree);
        if (this.rotationAngle > 0) {
            this.sensorHoming.setDetectedInput(false);
            this.rotationTask = executor.schedule(() -> {
                        this.rotationAngle = degree;
                    },
                    delay, TimeUnit.MILLISECONDS);
        } else {
            this.rotationTask = executor.schedule(() -> {
                        this.rotationAngle = degree;
                        this.sensorHoming.setDetectedInput(true);
                    },
                    delay, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public int getRotationAngle() {
        return this.rotationAngle;
    }

    @Override
    public void waitMs(long period) {
        super.waitMs(period);
    }
}
