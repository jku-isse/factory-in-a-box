package hardware;

import com.github.oxo42.stateless4j.StateMachine;
import functionalUnits.ConveyorBase;
import hardware.actuators.MockMotor;
import hardware.sensors.MockSensor;
import lombok.Getter;
import lombok.Setter;
import stateMachines.conveyor.ConveyorStateMachineConfig;
import stateMachines.conveyor.ConveyorStates;
import stateMachines.conveyor.ConveyorTriggers;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static stateMachines.conveyor.ConveyorStates.STOPPED;
import static stateMachines.conveyor.ConveyorTriggers.*;

/**
 * This class should be used to simulate the behaviour of the conveyor funit.
 * The simulation has motors and sensors that interact with each other.
 * If you want to assign this to a funit, use the motors exposed though the getters.
 * Needs support for supporting error scenarios for testing.
 */
public class ConveyorMock extends ConveyorBase {

    @Getter @Setter private MockMotor conveyorMotor;
    @Getter @Setter private MockSensor sensorLoading;
    @Getter @Setter private MockSensor sensorUnloading;

    private ScheduledThreadPoolExecutor executor;
    private ScheduledFuture loadingTask;
    private ScheduledFuture unloadingTask;

    @Getter @Setter private long timerMs;

    @Getter private final StateMachine<ConveyorStates, ConveyorTriggers> conveyorStateMachine;

    @Getter @Setter private AtomicBoolean fullyLoaded;
    @Getter @Setter private AtomicBoolean fullyUnloaded;

    public ConveyorMock(long timerMs) {
        this.executor = new ScheduledThreadPoolExecutor(4);
        this.fullyLoaded = new AtomicBoolean(false);
        this.fullyUnloaded = new AtomicBoolean(true);
        this.timerMs = timerMs;
        this.conveyorStateMachine = new StateMachine<>(STOPPED, new ConveyorStateMachineConfig());
        this.sensorLoading = new MockSensor() {
            @Getter @Setter private boolean detectedInput = false;

            @Override
            public boolean hasDetectedInput() {
                return detectedInput;
            }
        };
        this.sensorUnloading = new MockSensor() {

            @Getter @Setter private boolean detectedInput = false;

            @Override
            public boolean hasDetectedInput() {
                return detectedInput;
            }

        };
        this.conveyorMotor = new MockMotor(100) {
            @Getter @Setter private ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1);
            @Getter @Setter private ScheduledFuture forwardTask;
            @Getter @Setter private ScheduledFuture backwardTask;
            @Getter @Setter private int motorSpeed;
            @Getter @Setter private AtomicBoolean running = new AtomicBoolean(false);

            @Override
            public void forward() {
                executor.schedule(() -> {
                    sensorLoading.setDetectedInput(true);
                    fullyLoaded.set(true);
                }, timerMs, TimeUnit.MILLISECONDS);
                running.set(true);
                forwardTask = executorService.scheduleAtFixedRate(
                        () -> System.out.println("===| Moving forward with speed: " + motorSpeed),
                        0, 1000, TimeUnit.MILLISECONDS);
                fullyUnloaded.set(false);
            }

            @Override
            public void backward() {
                sensorUnloading.setDetectedInput(true);
                executor.schedule(() -> {
                    sensorUnloading.setDetectedInput(false);
                    fullyUnloaded.set(true);
                }, timerMs, TimeUnit.MILLISECONDS);
                running.set(true);
                backwardTask = executorService.scheduleAtFixedRate(
                        () -> System.out.println("===| Moving backward with speed: " + motorSpeed),
                        0, 1000, TimeUnit.MILLISECONDS);
                fullyLoaded.set(false);
            }

            @Override
            public void stop() {
                if (forwardTask != null) {
                    forwardTask.cancel(true);
                }
                if (backwardTask != null) {
                    backwardTask.cancel(true);
                }
                System.out.println("Motor stopped");
                running.set(false);
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
        };
    }

    @Override
    public void load() {
        if (!conveyorStateMachine.canFire(LOAD)) {
            System.out.println("Conveyor already fully loaded!");
            return;
        }
        conveyorStateMachine.fire(LOAD);
        getConveyorMotor().forward();
        getSensorUnloading().setDetectedInput(true);
        loadingTask = executor.schedule(() -> {
            getSensorLoading().setDetectedInput(true);
            getConveyorMotor().stop();
            conveyorStateMachine.fire(NEXT);
        }, timerMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void unload() {
        if (fullyUnloaded.get()) {
            System.out.println("Conveyor already empty!");
            return;
        }
        conveyorStateMachine.fire(UNLOAD);
        getConveyorMotor().backward();
        getSensorLoading().setDetectedInput(false);
        unloadingTask = executor.schedule(() -> {
            getSensorUnloading().setDetectedInput(false);
            getConveyorMotor().stop();
            conveyorStateMachine.fire(NEXT);
        }, timerMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void pause() {
        cancelSensorTasks();
        getConveyorMotor().stop();
        executor.schedule(() -> conveyorStateMachine.fire(PAUSE), timerMs, TimeUnit.MILLISECONDS);

    }

    @Override
    public void reset() {
        conveyorStateMachine.fire(RESET);
        cancelSensorTasks();
        getConveyorMotor().stop();
        executor.schedule(() -> conveyorStateMachine.fire(NEXT), timerMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        conveyorStateMachine.fire(STOP);
        cancelSensorTasks();
        getConveyorMotor().stop();
        executor.schedule(() -> conveyorStateMachine.fire(NEXT), timerMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void addServerConfig() {
        System.out.println("Adding Server config");
    }

    private void cancelSensorTasks(){
        if (loadingTask != null) {
            loadingTask.cancel(true);
            fullyLoaded.set(false);
        }
        if (unloadingTask != null) {
            unloadingTask.cancel(true);
            fullyUnloaded.set(false);
        }
    }
}
