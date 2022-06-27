package hardware.actuators;


import hardware.sensors.MockSensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TurningMockMotor extends MockMotor {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private MockSensor sensorHoming;
    private long delay;
    private boolean isTurningForward, isTurningBackward;
    private ScheduledThreadPoolExecutor executor;
    private ScheduledFuture timerTask;

    public TurningMockMotor(MockSensor sensorHoming, int speed) {
        super(speed);
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
        logger.info("Calling forward on ConveyorMockMotor...");
        isTurningForward = true;
        isTurningBackward = false;
    }

    @Override
    public void backward() {
        super.backward();
        logger.info("Calling backward on ConveyorMockMotor...");
        isTurningForward = false;
        isTurningBackward = true;
        timerTask = executor.schedule(() -> {
                    sensorHoming.setDetectedInput(true);
                }, delay, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        logger.info("Calling stop on ConveyorMockMotor...");
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
