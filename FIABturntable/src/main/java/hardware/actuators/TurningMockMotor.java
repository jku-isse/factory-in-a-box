package hardware.actuators;


import hardware.sensors.MockSensor;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TurningMockMotor extends MockMotor {

    private MockSensor sensorHoming;
    private long delay;
    private boolean isTurningForward, isTurningBackward;
    private ScheduledThreadPoolExecutor executor;
    private ScheduledFuture timerTask;

    public TurningMockMotor(int speed) {
        super(speed);
        this.delay = speed * 10;     //simulate time for turning, assuming speed >= 100 (1s) and speed <= 500 (5s)
        isTurningForward = false;
        isTurningBackward = false;
        executor = new ScheduledThreadPoolExecutor(2);
    }

    public void setSensorHoming(MockSensor sensorHoming) {
        this.sensorHoming = sensorHoming;
        sensorHoming.setDetectedInput(false);
    }

    @Override
    public void forward() {
        super.forward();
        isTurningForward = true;
        isTurningBackward = false;
    }

    @Override
    public void backward() {
        super.backward();
        isTurningForward = false;
        isTurningBackward = true;
        timerTask = executor.schedule(() -> sensorHoming.setDetectedInput(true),
                delay, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        super.stop();
        if (timerTask != null) {
            timerTask.cancel(true);
        }
        isTurningForward = false;
        isTurningBackward = false;
    }

    @Override
    public void waitMs(long period) {
        super.waitMs(period);
    }
}
