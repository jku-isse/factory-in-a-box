package functionalUnits.functionalUnitMock;

import com.github.oxo42.stateless4j.StateMachine;
import functionalUnits.ConveyorBase;
import hardware.actuators.MockMotor;
import hardware.sensors.MockSensor;
import lombok.Getter;
import lombok.Setter;
import stateMachines.conveyor.ConveyorStateMachineConfig;
import stateMachines.conveyor.ConveyorStates;
import stateMachines.conveyor.ConveyorTriggers;

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

    @Getter @Setter private AtomicBoolean fullyLoaded;
    @Getter @Setter private AtomicBoolean fullyUnloaded;

    public ConveyorMock(long timerMs) {
        this.executor = new ScheduledThreadPoolExecutor(4);
        this.fullyLoaded = new AtomicBoolean(false);
        this.fullyUnloaded = new AtomicBoolean(true);
        this.timerMs = timerMs;
        this.conveyorStateMachine = new StateMachine<>(STOPPED, new ConveyorStateMachineConfig());
        this.sensorLoading = new MockSensor();
        this.sensorUnloading = new MockSensor();
        this.conveyorMotor = new MockMotor(100);
        this.conveyorMotor.setSensorLoading(sensorLoading);     //TODO refactor
        this.conveyorMotor.setSensorUnloading(sensorUnloading);
    }

    @Override
    public void load() {
        if (!conveyorStateMachine.canFire(LOAD)) {
            System.out.println("Conveyor already fully loaded!");
            return;
        }
        conveyorStateMachine.fire(LOAD);
        getConveyorMotor().backward();
        getSensorUnloading().setDetectedInput(true);
        fullyUnloaded.set(false);
        loadingTask = executor.schedule(() -> {
            getSensorLoading().setDetectedInput(true);
            getConveyorMotor().stop();
            fullyLoaded.set(true);
            conveyorStateMachine.fire(NEXT);
        }, timerMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void unload() {
        if(!conveyorStateMachine.canFire(UNLOAD)){
            System.out.println("Cannot unload empty conveyor");
            return;
        }
        conveyorStateMachine.fire(UNLOAD);
        getConveyorMotor().forward();
        getSensorLoading().setDetectedInput(false);
        fullyLoaded.set(false);
        unloadingTask = executor.schedule(() -> {
            getSensorUnloading().setDetectedInput(false);
            getConveyorMotor().stop();

            fullyUnloaded.set(true);
            conveyorStateMachine.fire(NEXT);
        }, timerMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void pause() {
        cancelSensorTasks();
        getConveyorMotor().stop();
        conveyorStateMachine.fire(PAUSE);
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

    private void cancelSensorTasks() {
        if (loadingTask != null) {
            loadingTask.cancel(true);
            fullyLoaded.set(false);
        }
        if (unloadingTask != null) {
            unloadingTask.cancel(true);
            fullyUnloaded.set(false);
        }
    }

    public StateMachine<ConveyorStates, ConveyorTriggers> getConveyorStateMachine() {
        return conveyorStateMachine;
    }
}
