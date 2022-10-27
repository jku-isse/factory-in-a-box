package hardware.actuators;


import hardware.sensors.MockSensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ConveyorMockMotor extends MockMotor {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final MockSensor sensorLoading;
    private final MockSensor sensorUnloading;

    private ScheduledFuture<?> loadingTask;
    private ScheduledFuture<?> unloadingTask;
    private final long delay;

    public ConveyorMockMotor(MockSensor sensorLoading, MockSensor sensorUnloading, int speed, long delay) {
        super(speed);
        this.sensorLoading = sensorLoading;
        this.sensorUnloading = sensorUnloading;
        this.delay = delay;
        logger.info("Initialized Conveyor Mock motor");
    }

    @Override
    public void forward() {
        super.forward();
        logger.info("Calling forward on ConveyorMockMotor...");
        if (sensorLoading != null && sensorUnloading != null) {
            sensorLoading.setDetectedInput(false);
            unloadingTask = executorService.schedule(() -> sensorUnloading.setDetectedInput(false), delay, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void backward() {
        super.backward();
        logger.info("Calling backward on ConveyorMockMotor...");
        if (sensorLoading != null && sensorUnloading != null) {
            sensorUnloading.setDetectedInput(true);
            loadingTask = executorService.schedule(() -> sensorLoading.setDetectedInput(true),
                    delay, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void stop() {
        super.stop();
        logger.info("Calling stop on ConveyorMockMotor...");
        if (loadingTask != null) {
            loadingTask.cancel(true);
        }
        if (unloadingTask != null) {
            unloadingTask.cancel(true);
        }
    }
}
