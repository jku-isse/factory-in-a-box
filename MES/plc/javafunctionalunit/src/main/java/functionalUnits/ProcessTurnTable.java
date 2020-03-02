package functionalUnits;

import com.github.oxo42.stateless4j.StateMachine;
import communication.utils.Pair;
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

    private void waitForTargetValue(String serverUrl, Pair<Integer, String> nodeId, int targetValue) {
        int val;
        while ((val = getClientCommunication().clientReadIntValueById(serverUrl, nodeId)) != targetValue) {
            if (isStopped()) {
                System.out.println("Process was interrupted.");
                return;
            }
            try {
                System.out.println("Reading value, waiting for " + nodeId.getValue() + " to be " + targetValue + ", val is: " + val);
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
        String[] info = fromTo.split(";");
        if (inputInvalid(info)) {
            System.out.println("Input not well defined");
            return;
        }
        String serverUrl = info[0];
        String from = info[1];
        String to = info[2];
        Pair<Integer, String> conveyorNode = new Pair<>(1, "CONVEYOR");
        Pair<Integer, String> turningNode = new Pair<>(1, "TURNING");
        Pair<Integer, String> conveyorStateNode = new Pair<>(1, "CONVEYOR_STATE");
        Pair<Integer, String> turningStateNode = new Pair<>(1, "TURNING_STATE");
        //Pair<Integer, String> handShakeServerState = new Pair<>(1, "HANDSHAKE_FU_STATE");     //Reset handshake fu
        //Wait for reset
        //Replace resets with global reset
        getClientCommunication().callStringMethod(serverUrl, conveyorNode, new Pair<>(1, "CONVEYOR_RESET"), "");         //Reset conveyor
        waitForTargetValue(serverUrl,conveyorStateNode, 0);
        getClientCommunication().callStringMethod(serverUrl, turningNode, new Pair<>(1, "TURNING_RESET"), "");          //Reset turning
        waitForTargetValue(serverUrl,turningStateNode, 0);                                 //Wait for idle turning state
        getClientCommunication().callStringMethod(serverUrl, turningNode, new Pair<>(1, "TURNING_TURN_TO"), from); //turning turn to (random)
        waitForTargetValue(serverUrl,turningStateNode, 0);                                 //Wait for turning complete state
        //Initiate handover client
        //wait for handshake to finish
        getClientCommunication().callStringMethod(serverUrl, conveyorNode, new Pair<>(1, "CONVEYOR_LOAD"), "");         //load conveyor
        waitForTargetValue(serverUrl,conveyorStateNode, 6);                                //wait for loaded state
        //Stop handshake
        //Reset handshake
        getClientCommunication().callStringMethod(serverUrl, turningNode, new Pair<>(1, "TURNING_RESET"), "");
        waitForTargetValue(serverUrl,turningStateNode, 0);                                 //Wait for idle turning state
        getClientCommunication().callStringMethod(serverUrl, turningNode, new Pair<>(1, "TURNING_TURN_TO"), to);           //turning turn to (random)
        waitForTargetValue(serverUrl,turningStateNode, 0);                                 //Wait for turning complete state
        //initiate handover client
        //wait for handshake to finish
        getClientCommunication().callStringMethod(serverUrl, conveyorNode, new Pair<>(1, "CONVEYOR_UNLOAD"), "");      //unload conveyor
        waitForTargetValue(serverUrl,conveyorStateNode, 0);                                //wait for conveyor idle state
        getClientCommunication().callStringMethod(serverUrl, conveyorNode, new Pair<>(1, "CONVEYOR_STOP"), "");       //stop conveyor
        waitForTargetValue(serverUrl,conveyorStateNode, 9);                               //wait for conveyor stopped state
        getClientCommunication().callStringMethod(serverUrl, turningNode, new Pair<>(1, "TURNING_STOP"), "");        //stop turning
        waitForTargetValue(serverUrl,turningStateNode, 7);                                 //wait for turning stopped state*/
        //Stop handshake
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

    public enum ProcessEngineStringIdentifiers {
        STATE, LOAD, RESET, STOP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addServerConfig() {
        final String PREFIX = "PROCESS_ENGINE_";
        statusNodeId = getServerCommunication().addIntegerVariableNode(getServer(), getObject(),
                new Pair<>(1, PREFIX + ProcessEngineStringIdentifiers.STATE.name()),
                PREFIX + ProcessEngineStringIdentifiers.STATE.name());
        getServerCommunication().addStringMethod(getServerCommunication(), getServer(), getObject(),
                new Pair<>(1, PREFIX + ProcessEngineStringIdentifiers.LOAD.name()),
                PREFIX + ProcessEngineStringIdentifiers.LOAD.name(), input -> {
                    loadProcess(input);
                    return "ProcessEngine: Load Process Successful";
                });
        getServerCommunication().addStringMethod(getServerCommunication(), getServer(), getObject(),
                new Pair<>(1, PREFIX + ProcessEngineStringIdentifiers.RESET.name()),
                PREFIX + ProcessEngineStringIdentifiers.RESET.name(), input -> {
                    reset();
                    return "ProcessEngine: Resetting Successful";
                });
        getServerCommunication().addStringMethod(getServerCommunication(), getServer(), getObject(),
                new Pair<>(1, PREFIX + ProcessEngineStringIdentifiers.STOP.name()),
                PREFIX + ProcessEngineStringIdentifiers.STOP.name(), input -> {
                    stop();
                    return "ProcessEngine: Stopping Successful";
                });
        updateState();
    }
}
