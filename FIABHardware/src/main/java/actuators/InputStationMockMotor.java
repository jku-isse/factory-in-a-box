package actuators;

import sensors.MockSensor;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class InputStationMockMotor extends MockMotor {

    private MockSensor palletSensor;
    private ScheduledFuture unloadingTask;
    private ScheduledFuture reloadingTask;
    private final int DELAY_IN_MS = 7000;      //just to test monitoring, lower the value for faster handover

    public InputStationMockMotor() {
        super(200);
    }

    public void setPalletSensor(MockSensor palletSensor) {
        this.palletSensor = palletSensor;
        this.palletSensor.setDetectedInput(true);
    }

    public MockSensor getPalletSensor() {
        return palletSensor;
    }

    @Override
    public void forward() {
        super.forward();
        if (palletSensor != null) {
            unloadingTask = executorService.schedule(() -> {
                palletSensor.setDetectedInput(false);
                System.out.println("Pallet sensor value is now: " + palletSensor.hasDetectedInput());
            }, DELAY_IN_MS, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void backward() {
        super.backward();
        if (palletSensor != null) {
            reloadingTask = executorService.schedule(() -> palletSensor.setDetectedInput(true), DELAY_IN_MS, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void stop() {
        super.stop();
        if (reloadingTask != null) {
            reloadingTask.cancel(true);
        }
        if (unloadingTask != null) {
            unloadingTask.cancel(true);
        }
        if (palletSensor != null) {
            palletSensor.setDetectedInput(true);
        }
    }
}
