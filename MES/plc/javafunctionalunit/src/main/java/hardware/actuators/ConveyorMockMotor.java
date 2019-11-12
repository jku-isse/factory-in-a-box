package hardware.actuators;

import hardware.sensors.MockSensor;
import lombok.Setter;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ConveyorMockMotor extends MockMotor {

    @Setter private MockSensor sensorLoading;
    @Setter private MockSensor sensorUnloading;

    private ScheduledFuture loadingTask;
    private ScheduledFuture unloadingTask;
    private long delay;

    public ConveyorMockMotor(int speed, long delay) {
        super(speed);
        this.delay = delay;
    }

    @Override
    public void forward() {
        super.forward();
        if (sensorLoading != null && sensorUnloading != null) {
            sensorLoading.setDetectedInput(false);
            unloadingTask = executorService.schedule(() -> sensorUnloading.setDetectedInput(false), delay, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void backward() {
        super.backward();
        if (sensorLoading != null && sensorUnloading != null) {
            sensorUnloading.setDetectedInput(true);
            loadingTask = executorService.schedule(() -> sensorLoading.setDetectedInput(true), delay, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void stop() {
        super.stop();
        if (loadingTask != null) {
            loadingTask.cancel(true);
        }
        if (unloadingTask != null) {
            unloadingTask.cancel(true);
        }
    }
}
