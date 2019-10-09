package functionalUnits.functionalUnitTurnTable;

import com.github.oxo42.stateless4j.StateMachine;
import communication.utils.RequestedNodePair;
import functionalUnits.ProcessEngineBase;
import io.vertx.core.Vertx;
import stateMachines.processEngine.ProcessEngineStateMachineConfig;
import stateMachines.processEngine.ProcessEngineStates;
import stateMachines.processEngine.ProcessEngineTriggers;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import static stateMachines.processEngine.ProcessEngineStates.STOPPED;
import static stateMachines.processEngine.ProcessEngineTriggers.*;

/**
 * TurnTable implementation of the Process Engine
 */
public class ProcessTurnTable extends ProcessEngineBase {

    private Object statusNodeId;
    private AtomicBoolean stopped;
    private final StateMachine<ProcessEngineStates, ProcessEngineTriggers> processEngineStateMachine;
    private Random random;

    public ProcessTurnTable() {
        stopped = new AtomicBoolean(false);
        processEngineStateMachine = new StateMachine<>(STOPPED, new ProcessEngineStateMachineConfig());
        random = new Random();
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
     * This method currently mocks a process. Processes should be parsed and converted to commands.
     */
    @Override
    public void loadProcess() {
        Vertx vertx = Vertx.vertx();
        vertx.executeBlocking(promise -> {
            if (isStopped() || !processEngineStateMachine.canFire(EXECUTE)) {
                System.out.println("Reset Process Engine to load a process");
                return;
            }
            processEngineStateMachine.fire(EXECUTE);
            updateState();
            getClientCommunication().callStringMethod(getServerUrl(), new RequestedNodePair<>(1, 20),

                    new RequestedNodePair<>(1, 23), "");
            System.out.println("Successfully reset conveyor");
            getClientCommunication().callStringMethod(getServerUrl(), new RequestedNodePair<>(1, 30),
                    new RequestedNodePair<>(1, 31), "");
            System.out.println("Successfully reset turning");
            //wait for updated status variable
            while (getClientCommunication().getTurningStatus() != 0) {
                if (isStopped()) {
                    System.out.println("Process was interrupted.");
                    return;
                }
            }
            //Load the conveyor
            getClientCommunication().callStringMethod(getServerUrl(), new RequestedNodePair<>(1, 20),


                    new RequestedNodePair<>(1, 21), "");
            //Wait for loading to finish
            while (getClientCommunication().getConveyorStatus() != 6) {
                if (isStopped()) {
                    System.out.println("Process was interrupted.");
                    return;
                }
            }

            //Create a random number between 1-3 to turn the conveyor in different directions
            String direction = String.valueOf(random.nextInt(3) + 1);
            getClientCommunication().callStringMethod(getServerUrl(), new RequestedNodePair<>(1, 30),
                    new RequestedNodePair<>(1, 33), direction);
            //Wait for turning to finish

            while (getClientCommunication().getTurningStatus() != 5) {
                if (isStopped()) {
                    System.out.println("Process was interrupted.");
                    return;
                }
            }
            //Unload conveyor
            getClientCommunication().callStringMethod(getServerUrl(), new RequestedNodePair<>(1, 20),


                    new RequestedNodePair<>(1, 25), "");
            System.out.println("Successfully unloaded");
            //Wait to finish unloading
            while (getClientCommunication().getConveyorStatus() != 0) {
                if (isStopped()) {
                    System.out.println("Process was interrupted.");
                    return;
                }
            }
            //Stop Turning funit
            getClientCommunication().callStringMethod(getServerUrl(), new RequestedNodePair<>(1, 20),


                    new RequestedNodePair<>(1, 24), "");
            //Wait for reset to finish
            while (getClientCommunication().getTurningStatus() != 5) {
                if (isStopped()) {
                    System.out.println("Process was interrupted.");
                    return;
                }
            }
            //Stop conveyor
            getClientCommunication().callStringMethod(getServerUrl(), new RequestedNodePair<>(1, 30),
                    new RequestedNodePair<>(1, 32), "");
            processEngineStateMachine.fire(NEXT);
            updateState();
        }, res -> {
        });
        vertx.close();
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
        processEngineStateMachine.fire(NEXT);
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
        updateState();
    }
}
