package functionalUnits.functionalUnitTurnTable;

import com.github.oxo42.stateless4j.StateMachine;
import communication.utils.RequestedNodePair;
import functionalUnits.ProcessEngineBase;
import stateMachines.processEngine.ProcessEngineStateMachineConfig;
import stateMachines.processEngine.ProcessEngineStates;
import stateMachines.processEngine.ProcessEngineTriggers;

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
        if (!processEngineStateMachine.canFire(EXECUTE)) {
            return;
        }
        new Thread(() -> {
            processEngineStateMachine.fire(EXECUTE);
            updateState();
            //TODO refactor once it works with multiple Functional Units
            //System.out.println("Conveyor status: " + getClientCommunication().getConveyorStatus());
            if (getClientCommunication().getConveyorStatus() != 9 || getClientCommunication().getTurningStatus() != 7) {
                System.out.println("Busy");
                return;
            } else if (getClientCommunication().getConveyorStatus() == -1 || getClientCommunication().getTurningStatus() == -1) {
                //Stop conveyor to update state to stopped if not initialised
                System.out.println("Resetting");
                getClientCommunication().callStringMethod(getClient(), new RequestedNodePair<>(1, 20),
                        new RequestedNodePair<>(1, 24), "");
                getClientCommunication().callStringMethod(getClient(), new RequestedNodePair<>(1, 30),
                        new RequestedNodePair<>(1, 32), "");
            }
            System.out.println("Entered load process");
            int conveyorState = getClientCommunication().getConveyorStatus();
            int turningState = getClientCommunication().getTurningStatus();
            System.out.println("Loading Process");
            //Reset conveyor
            System.out.println("Resetting conveyor");
            getClientCommunication().callStringMethod(getClient(), new RequestedNodePair<>(1, 20),
                    new RequestedNodePair<>(1, 23), "");
            System.out.println("Reset conveyor done");

            while (conveyorState != 0) {
                if (isStopped()) {
                    return;
                }
                conveyorState = getClientCommunication().getConveyorStatus();
            }
            //Reset turning
            System.out.println("Resetting turning");
            getClientCommunication().callStringMethod(getClient(), new RequestedNodePair<>(1, 30),
                    new RequestedNodePair<>(1, 31), "");
            System.out.println("Reset turning done");

            while (turningState != 0) {
                if (isStopped()) {
                    return;
                }
                turningState = getClientCommunication().getTurningStatus();
            }
            //Load conveyor
            System.out.println("Loading conveyor");
            getClientCommunication().callStringMethod(getClient(), new RequestedNodePair<>(1, 20),
                    new RequestedNodePair<>(1, 21), "");
            System.out.println("Loading done");

            while (conveyorState != 6) {
                if (isStopped()) {
                    return;
                }
                conveyorState = getClientCommunication().getConveyorStatus();
            }
            //TurnTo
            System.out.println("Turn to East");
            getClientCommunication().callStringMethod(getClient(), new RequestedNodePair<>(1, 30),
                    new RequestedNodePair<>(1, 33), "1");
            System.out.println("Turning East done");
            turningState = getClientCommunication().getTurningStatus(); //To avoid 0 from reset
            while (turningState != 0) {
                if (isStopped()) {
                    return;
                }
                turningState = getClientCommunication().getTurningStatus();
            }
            //Unload conveyor
            System.out.println("Unloading conveyor");
            getClientCommunication().callStringMethod(getClient(), new RequestedNodePair<>(1, 20),
                    new RequestedNodePair<>(1, 25), "");
            System.out.println("Unloading done");

            while (conveyorState != 0) {
                if (isStopped()) {
                    return;
                }
                conveyorState = getClientCommunication().getConveyorStatus();
            }
            //Stop conveyor
            System.out.println("Stopping conveyor");
            getClientCommunication().callStringMethod(getClient(), new RequestedNodePair<>(1, 20),
                    new RequestedNodePair<>(1, 24), "");
            System.out.println("Stop done");
            while (conveyorState != 9) {
                if (isStopped()) {
                    return;
                }
                conveyorState = getClientCommunication().getConveyorStatus();
            }
            //Stop turning
            System.out.println("Stopping turning");
            getClientCommunication().callStringMethod(getClient(), new RequestedNodePair<>(1, 30),
                    new RequestedNodePair<>(1, 33), "");
            System.out.println("Stopping done");

            while (turningState != 7) {
                if (isStopped()) {
                    return;
                }
                turningState = getClientCommunication().getTurningStatus();
            }
            processEngineStateMachine.fire(NEXT);
            updateState();
            System.out.println("Loaded Process successfully");
        }).start();
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
