package functionalUnits.functionalUnitTurnTable;

import com.github.oxo42.stateless4j.StateMachine;
import communication.utils.RequestedNodePair;
import functionalUnits.ProcessEngineBase;
import stateMachines.processEngine.ProcessEngineStateMachineConfig;
import stateMachines.processEngine.ProcessEngineStates;
import stateMachines.processEngine.ProcessEngineTriggers;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static stateMachines.processEngine.ProcessEngineStates.STOPPED;
import static stateMachines.processEngine.ProcessEngineTriggers.RESET;
import static stateMachines.processEngine.ProcessEngineTriggers.STOP;

/**
 * TurnTable implementation of the Process Engine
 */
public class ProcessTurnTable extends ProcessEngineBase {

    private Object statusNodeId;
    private AtomicBoolean stopped;
    private final StateMachine<ProcessEngineStates, ProcessEngineTriggers> processEngineStateMachine;

    public ProcessTurnTable() {
        stopped = new AtomicBoolean(false);
        processEngineStateMachine = new StateMachine<>(STOPPED, new ProcessEngineStateMachineConfig());
    }

    private boolean isStopped() {
        return stopped.get();
    }

    /**
     * Updates the state on the server
     */
    private void updateState() {
        getServerCommunication().writeVariable(getServer(), statusNodeId, processEngineStateMachine.getState().getValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadProcess() {
        if (isStopped()) {
            System.out.println("Reset Process Engine");
            return;
        }
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        Thread resetConveyor = new Thread(() -> {
            getClientCommunication().callStringMethod(getClient(), new RequestedNodePair<>(1, 20),
                    new RequestedNodePair<>(1, 23), "");
            System.out.println("Successfully reset conveyor");
        });
        Thread resetTurning = new Thread(() -> {getClientCommunication().callStringMethod(getClient(), new RequestedNodePair<>(1, 30),
                new RequestedNodePair<>(1, 31), "");
            System.out.println("Successfully reset turning");});
        Thread loading = new Thread(() -> {getClientCommunication().callStringMethod(getClient(), new RequestedNodePair<>(1, 20),
                new RequestedNodePair<>(1, 21), "");
            System.out.println("Successfully loaded");});
        Thread turnSouth = new Thread(() -> {
            while (getClientCommunication().getConveyorStatus() != 6) {
                if (isStopped()) {
                    System.out.println("Process was interrupted.");
                    return;
                }
            }
            getClientCommunication().callStringMethod(getClient(), new RequestedNodePair<>(1, 30),
                    new RequestedNodePair<>(1, 33), "2");
            System.out.println("Successfully turned south");
        });
        Thread unloading = new Thread(() -> {
            while (getClientCommunication().getTurningStatus() != 0) {
                if (isStopped()) {
                    System.out.println("Process was interrupted.");
                    return;
                }
            }
            getClientCommunication().callStringMethod(getClient(), new RequestedNodePair<>(1, 20),
                    new RequestedNodePair<>(1, 25), "");
            System.out.println("Successfully unloaded");
        });
        Thread stopConveyor = new Thread(() -> {
            while (getClientCommunication().getConveyorStatus() != 0) {
                if (isStopped()) {
                    System.out.println("Process was interrupted.");
                    return;
                }
            }
            getClientCommunication().callStringMethod(getClient(), new RequestedNodePair<>(1, 20),
                    new RequestedNodePair<>(1, 24), "");
            System.out.println("Successfully stopped conveyor");
        });
        Thread stopTurning = new Thread(() -> {
            while (getClientCommunication().getTurningStatus() != 0) {
                if (isStopped()) {
                    System.out.println("Process was interrupted.");
                    return;
                }
            }
            getClientCommunication().callStringMethod(getClient(), new RequestedNodePair<>(1, 30),
                    new RequestedNodePair<>(1, 32), "");
            System.out.println("Successfully stopped turning");

        });
        executorService.submit(resetConveyor);
        executorService.schedule(resetTurning, 500, TimeUnit.MILLISECONDS);
        executorService.schedule(loading, 1000, TimeUnit.MILLISECONDS);
        executorService.schedule(turnSouth, 1500, TimeUnit.MILLISECONDS);
        executorService.schedule(unloading, 2000, TimeUnit.MILLISECONDS);
        executorService.schedule(stopConveyor, 2500, TimeUnit.MILLISECONDS);
        executorService.schedule(stopTurning, 3000, TimeUnit.MILLISECONDS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        if (!processEngineStateMachine.canFire(RESET)) {
            return;
        }
        processEngineStateMachine.fire(RESET);
        updateState();
        stopped.set(false);
        System.out.println("Reset Process was called");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (!processEngineStateMachine.canFire(STOP)) {
            return;
        }
        processEngineStateMachine.fire(STOP);
        updateState();
        stopped.set(true);
        System.out.println("Stop Process was called");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addServerConfig() {
        statusNodeId = getServerCommunication().addIntegerVariableNode(getServer(), getObject(),
                new RequestedNodePair<>(1, 58), "ProcessEngineStatus");
        addStringMethodToServer(new RequestedNodePair<>(1, 41), "LoadProcessMethod", x -> {
            loadProcess();
            return "Load Process Successful";
        });
        addStringMethodToServer(new RequestedNodePair<>(1, 42), "ResetProcessMethod", x -> {
            reset();
            return "Resetting Successful";
        });
        addStringMethodToServer(new RequestedNodePair<>(1, 43), "StopProcessMethod", x -> {
            stop();
            return "Stopping Successful";
        });
    }
}
