package hardware.actuators;

import hardware.sensors.MockSensor;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TurningMockMotor extends MockMotor {

    private MockSensor sensorHoming;
    private long delay;
    private long timeSpentTurning;
    private boolean isTurningForward, isTurningBackward;
    private ScheduledThreadPoolExecutor executor;
    private ScheduledFuture timerTask;

    public TurningMockMotor(MockSensor sensorHoming, int speed) {
        super(speed);
        this.sensorHoming = sensorHoming;
        this.delay = delay;
        timeSpentTurning = 0;
        isTurningForward = false;
        isTurningBackward = false;
        executor = new ScheduledThreadPoolExecutor(2);
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
                timeSpentTurning, TimeUnit.MILLISECONDS);
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
        if (isTurningForward) {
            timeSpentTurning += period;
        } else if (isTurningBackward) {
            timeSpentTurning -= period;
        }
    }
}
