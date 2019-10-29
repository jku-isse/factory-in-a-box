package hardware;

import hardware.actuators.Motor;
import hardware.sensors.Sensor;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class should be used to simulate the behaviour of the conveyor funit.
 * The simulation has motors and sensors that interact with each other.
 * If you want to assign this to a funit, use the motors exposed though the getters.
 * Needs support for supporting error scenarios for testing.
 */
public class ConveyorSimulation {

    private Motor conveyorMotor;
    private Sensor sensorLoading;
    private Sensor sensorUnloading;

    private ScheduledThreadPoolExecutor executor;
    private ScheduledFuture loadingTask;
    private ScheduledFuture unloadingTask;
    private long timerMs;

    private AtomicBoolean fullyLoaded;
    private AtomicBoolean fullyUnloaded;
    private AtomicBoolean stopped;

    public ConveyorSimulation(Motor conveyorMotor, Sensor sensorLoading, Sensor sensorUnloading, long timerMs) {
        executor = new ScheduledThreadPoolExecutor(2);  //one for each sensor
        this.fullyLoaded = new AtomicBoolean(false);
        this.fullyUnloaded = new AtomicBoolean(true);
        this.stopped = new AtomicBoolean(false);
        this.timerMs = timerMs;
        this.sensorLoading = new Sensor() {
            @Override
            public boolean hasDetectedInput() {
                return sensorLoading.hasDetectedInput();
            }

            @Override
            public void setDetectedInput(boolean detectedInput) {
                sensorLoading.setDetectedInput(detectedInput);
            }
        };
        this.sensorUnloading = new Sensor() {
            @Override
            public boolean hasDetectedInput() {
                return sensorUnloading.hasDetectedInput();
            }

            @Override
            public void setDetectedInput(boolean detectedInput) {
                sensorUnloading.setDetectedInput(detectedInput);
            }
        };
        this.conveyorMotor = new Motor() {
            @Override
            public void forward() {
                executor.schedule(() -> {
                    getSensorLoading().setDetectedInput(true);
                    fullyLoaded.set(true);
                }, timerMs, TimeUnit.MILLISECONDS);
                conveyorMotor.forward();
                fullyUnloaded.set(false);
            }

            @Override
            public void backward() {
                executor.schedule(() -> {
                    getSensorLoading().setDetectedInput(false);
                    fullyUnloaded.set(true);
                }, timerMs, TimeUnit.MILLISECONDS);
                conveyorMotor.backward();
                fullyLoaded.set(false);
            }

            @Override
            public void stop() {
                if (loadingTask != null) {
                    loadingTask.cancel(true);
                }
                if (unloadingTask != null) {
                    unloadingTask.cancel(true);
                }
                stopped.set(true);
                conveyorMotor.stop();
            }

            @Override
            public void setSpeed(int speed) {
                conveyorMotor.setSpeed(speed);
            }

            @Override
            public void waitMs(long msDelay) {
                conveyorMotor.waitMs(msDelay);
            }
        };

    }

    /**
     * Returns the conveyor motor
     *
     * @return conveyorMotor
     */
    public Motor getConveyorMotor() {
        return conveyorMotor;
    }

    /**
     * Returns the sensor used for loading
     *
     * @return sensorLoading
     */
    public Sensor getSensorLoading() {
        return sensorLoading;
    }

    /**
     * Returns the sensor used for unloading
     *
     * @return sensorUnloading
     */
    public Sensor getSensorUnloading() {
        return sensorUnloading;
    }
}
