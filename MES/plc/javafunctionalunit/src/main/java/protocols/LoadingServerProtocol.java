/**
 * [Class description.  The first sentence should be a meaningful summary of the class since it
 * will be displayed as the class summary on the Javadoc package page.]
 * <p>
 * [Other notes, including guaranteed invariants, usage instructions and/or examples, reminders
 * about desired improvements, etc.]
 *
 * @author Michael Bishara
 * @author <A HREF="mailto:[michaelbishara14@gmail.com]">[Michael Bishara]</A>
 * @author <A HREF="https://github.com/michaelanis14">[Github]</A>
 * @date 30 Oct 2019
 **/
package protocols;

import communication.open62communication.ServerCommunication;
import communication.utils.Pair;
import stateMachines.loadingProtocol.ServerLoadingStates;

public class LoadingServerProtocol {
    private static int currentState;
    private boolean conveyorOccupied;  // 0 for Idle , 1 for occupied
    private ServerCommunication serverCommunication;
    private Object opcua_server;
    private Object parentObject;
    Object state_nodeid;

    public LoadingServerProtocol() {
        conveyorOccupied = false; // 0 for Idle
    }

    public LoadingServerProtocol(ServerCommunication serverCommunication, Object server, Object parentObject,String nodePrefix) {
        conveyorOccupied = false; // 0 for Idle

        this.serverCommunication = serverCommunication;
        this.opcua_server = server;
        this.parentObject = parentObject;
        state_nodeid = serverCommunication.addStringVariableNode(opcua_server, parentObject, new Pair<>(1, nodePrefix+"_LOADING_SERVER_STATE"), "STATE");


        serverCommunication.addStringMethod(serverCommunication, server, parentObject, new Pair<>(1, nodePrefix+"REQUEST_INIT_HANDOVER"), "REQUEST_INIT_HANDOVER",
                opcuaMethodInput -> {
                    return request_init_handover();
                });

        serverCommunication.addStringMethod(serverCommunication, server, parentObject, new Pair<>(1, nodePrefix+"REQUEST_START_HANDOVER"), "REQUEST_START_HANDOVER",
                opcuaMethodInput -> {
                    return request_start_handover();
                });

        serverCommunication.addStringMethod(serverCommunication, server, parentObject, new Pair<>(1, nodePrefix+"LOADING_SERVER_COMPLETE"), "COMPLETE",
                opcuaMethodInput -> {
                    return complete();
                });
        serverCommunication.addStringMethod(serverCommunication, server, parentObject, new Pair<>(1, nodePrefix+"LOADING_SERVER_RESET"), "RESET",
                opcuaMethodInput -> {
                    return reset();
                });
        serverCommunication.addStringMethod(serverCommunication, server, parentObject, new Pair<>(1, nodePrefix+"LOADING_SERVER_STOP"), "STOP",
                opcuaMethodInput -> {
                    return stop();
                });
        serverCommunication.addStringMethod(serverCommunication, server, parentObject, new Pair<>(1, nodePrefix+"LOADING_SERVER_READY"), "READY",
                opcuaMethodInput -> {
                    return ready();
                });
    }


    private String request_start_handover() {
        if (getCurrentState() == ServerLoadingStates.READY_EMPTY.ordinal()) {
            changeState(ServerLoadingStates.EXECUTE);
            return "REQUEST_START_HANDOVER Successful";
        } else return "WRONG STATE NOT @ READY_EMPTY";
    }

    private String request_start_Unloading() {
        if (getCurrentState() == ServerLoadingStates.READY_LOADED.ordinal()) {
            changeState(ServerLoadingStates.EXECUTE);
            return "REQUEST_START_HANDOVER Successful";
        } else return "WRONG STATE NOT @ READY_LOADED";
    }

    private String stop() {
        if (getCurrentState() != ServerLoadingStates.STOPPING.ordinal() && getCurrentState() != ServerLoadingStates.STOPPED.ordinal()) {
            changeState(ServerLoadingStates.STOPPING);
            return "STOP Successful";
        } else return "WRONG STATE NOT @ WORKING";
    }

    private String reset() {
        if (getCurrentState() == ServerLoadingStates.STOPPED.ordinal()) {
            changeState(ServerLoadingStates.RESETTING);
            return "RESET Successful";
        } else return "WRONG STATE NOT @ STOPPED";
    }

    private String complete() {
        if (getCurrentState() == ServerLoadingStates.EXECUTE.ordinal()) {
            changeState(ServerLoadingStates.COMPLETING);
            return "COMPLETE Successful";
        } else return "WRONG STATE NOT @ EXECUTE";
    }


    private String request_init_Unloading() {
        if (getCurrentState() == ServerLoadingStates.IDLE_LOADED.ordinal()) {
            changeState(ServerLoadingStates.STARTING);
            return "REQUEST_INIT_HANDOVER Successful";
        } else return "WRONG STATE NOT @ IDLE_LOADED";
    }

    private String request_init_handover() {
        if (getCurrentState() == ServerLoadingStates.IDLE_EMPTY.ordinal()) {
            changeState(ServerLoadingStates.STARTING);
            return "REQUEST_INIT_HANDOVER Successful";
        } else return "WRONG STATE NOT @ IDLE_EMPTY";
    }

    public void changeState(ServerLoadingStates states) {

        switch (states) {
            case RESETTING:
                currentState = ServerLoadingStates.RESETTING.ordinal();
                resetting();
                break;
            case IDLE_EMPTY:
                currentState = ServerLoadingStates.IDLE_EMPTY.ordinal();
                idle_empty();
                break;
            case IDLE_LOADED:
                currentState = ServerLoadingStates.IDLE_LOADED.ordinal();
                idle_loaded();
                break;
            case STARTING:
                currentState = ServerLoadingStates.STARTING.ordinal();
                starting();
                break;
            case PREPARING:
                currentState = ServerLoadingStates.PREPARING.ordinal();
                preparing();
                break;
            case READY_LOADED:
                currentState = ServerLoadingStates.READY_LOADED.ordinal();
                ready_loaded();
                break;
            case READY_EMPTY:
                currentState = ServerLoadingStates.READY_EMPTY.ordinal();
                ready_empty();
                break;
            case EXECUTE:
                currentState = ServerLoadingStates.EXECUTE.ordinal();
                execute();
                break;
            case COMPLETING:
                currentState = ServerLoadingStates.COMPLETING.ordinal();
                completing();
                break;
            case COMPLETED:
                currentState = ServerLoadingStates.COMPLETED.ordinal();
                completed();
                break;
            case STOPPING:
                currentState = ServerLoadingStates.STOPPING.ordinal();
                stopping();
                break;
            case STOPPED:
                currentState = ServerLoadingStates.STOPPED.ordinal();
                stopped();
                break;


        }
        serverCommunication.writeVariable(opcua_server, state_nodeid, ServerLoadingStates.values()[currentState].toString());
    }


    private void starting() {

        changeState(ServerLoadingStates.PREPARING);
    }

    private void preparing() {
        //any internal actions to prepare loading
        if (!isConveyorOccupied()) { // idle
            changeState(ServerLoadingStates.READY_EMPTY);
        } else {
            changeState(ServerLoadingStates.READY_LOADED);
        }
    }
    private String ready() {
        return "";
    }
    private void ready_loaded() {
    }

    private void ready_empty() {
    }

    private void execute() {

        //replied with ok to the request
        setConveyorState(!isConveyorOccupied());
    }

    private void completing() {

        //State called form the method complete
        changeState(ServerLoadingStates.COMPLETED);
    }

    private void completed() {
        changeState(ServerLoadingStates.STOPPING);
    }

    private void stopping() {
        changeState(ServerLoadingStates.STOPPED);
    }

    private void stopped() {

        //Stopped STATE
    }

    private void idle_empty() {
    }

    private void idle_loaded() {
    }

    private void resetting() {
        if (!isConveyorOccupied()) { // idle
            changeState(ServerLoadingStates.IDLE_EMPTY);
        } else {
            changeState(ServerLoadingStates.IDLE_LOADED);
        }
    }

    public final int getCurrentState() {
        return currentState;
    }

    public boolean isConveyorOccupied() {
        return conveyorOccupied;
    }

    public void setConveyorState(boolean conveyorOccupied) {
        this.conveyorOccupied = conveyorOccupied;
    }


}
