package functionalUnits;

import com.github.oxo42.stateless4j.StateMachine;
import communication.utils.RequestedNodePair;
import functionalUnits.base.ProcessEngineBase;
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
    private final int DELAY = 1000;

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

    private void callMethod(String serverUrl, int namespace, int objectId, int methodId, String args) {
        getClientCommunication().callStringMethod(serverUrl, new RequestedNodePair<>(namespace, objectId),
                new RequestedNodePair<>(namespace, methodId), args);
        System.out.println("Method call successful");
    }

    private void waitForTargetValue(RequestedNodePair<Integer, Integer> nodeId, int targetValue) {
        while (getClientCommunication().clientReadIntValueById(getClient(), nodeId) != targetValue) {
            if (isStopped()) {
                System.out.println("Process was interrupted.");
                return;
            }
            try {
                Thread.sleep(DELAY);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean inputInvalid(String[] input) {
        return input.length != 3;
    }

    private void loadFromUnloadTo(String fromTo) {
        //TODO do not reset if current orientation is equal to from
        //TODO do not reset if from == to
        String[] info = fromTo.split(";");
        if (inputInvalid(info)) {
            System.out.println("Input not well defined");
            return;
        }
        String serverUrl = info[0];
        String from = info[1];
        String to = info[2];
        RequestedNodePair<Integer, Integer> conveyorNode = new RequestedNodePair<>(1, 56);
        RequestedNodePair<Integer, Integer> turningNode = new RequestedNodePair<>(1, 57);
        callMethod(serverUrl, 1, 20, 23, "");         //Reset conveyor
        callMethod(serverUrl, 1, 30, 31, "");         //Reset turning
        waitForTargetValue(turningNode, 0);                                 //Wait for idle turning state
        callMethod(serverUrl, 1, 30, 33, from);            //turning turn to (random)
        waitForTargetValue(turningNode, 0);                                 //Wait for turning complete state
        callMethod(serverUrl, 1, 20, 21, "");         //load conveyor
        waitForTargetValue(conveyorNode, 6);                                //wait for loaded state
        callMethod(serverUrl, 1, 30, 31, "");         //Reset turning
        waitForTargetValue(turningNode, 0);                                 //Wait for idle turning state
        callMethod(serverUrl, 1, 30, 33, to);              //turning turn to (random)
        waitForTargetValue(turningNode, 0);                                 //Wait for turning complete state
        callMethod(serverUrl, 1, 20, 25, "");         //unload conveyor
        waitForTargetValue(conveyorNode, 0);                                //wait for conveyor idle state
        callMethod(serverUrl, 1, 20, 24, "");        //stop conveyor
        waitForTargetValue(conveyorNode, 9);                               //wait for conveyor stopped state
        callMethod(serverUrl, 1, 30, 32, "");        //stop turning
        waitForTargetValue(turningNode, 7);                                 //wait for turning stopped state
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadProcess(String info) {      //Test using opc.tcp://localhost:4840;0;2 for example
        if (isStopped() || !processEngineStateMachine.canFire(EXECUTE)) {
            System.out.println("Reset Process Engine to load a process");
            return;
        }
        new Thread(() -> {
            System.out.println("loadProcess start");
            processEngineStateMachine.fire(EXECUTE);
            updateState();
            loadFromUnloadTo(info);
            processEngineStateMachine.fire(NEXT);
            updateState();
            System.out.println("loadProcess end");
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
            loadProcess(x);
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
