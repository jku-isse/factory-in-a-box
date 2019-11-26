package hardware.actuators;

import hardware.sensors.MockSensor;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ConveyorMockMotor extends MockMotor {

    private MockSensor sensorLoading;
    private MockSensor sensorUnloading;

    private ScheduledFuture loadingTask;
    private ScheduledFuture unloadingTask;
    private long delay;

    public ConveyorMockMotor(MockSensor sensorLoading, MockSensor sensorUnloading, int speed, long delay) {
        super(speed);
        this.sensorLoading = sensorLoading;
        this.sensorUnloading = sensorUnloading;
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

        sensorUnloading.setDetectedInput(true);
        System.out.println("Hello backward");
        loadingTask = executorService.schedule(() -> {
            System.out.println("Set loading sensor to true");
            sensorLoading.setDetectedInput(true);
        }, delay, TimeUnit.MILLISECONDS);

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
