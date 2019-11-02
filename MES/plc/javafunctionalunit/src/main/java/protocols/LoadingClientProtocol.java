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
 * @date 5 Sep 2019
 **/

package protocols;


import communication.open62communication.ClientCommunication;
import helper.ClientLoadingStates;

import static helper.ClientLoadingStates.*;

public class LoadingClientProtocol {
    int currentState;
    String serverPath;
    String orderId;

    Object opcua_client;
    ClientCommunication clientCommunication;

    public LoadingClientProtocol(ClientCommunication clientCommunication,Object opcua_client) {
		this.clientCommunication = clientCommunication;
        this.opcua_client = opcua_client;

    }

    public final int getCurrentState() {
        return currentState;
    }

    public void idle() {
        // comm.getClientCommunication().ClientSubtoNode(jClientAPIBase, client, nodeID)
    }

    public boolean starting() {
//subscribe
       // int connection_states;

        new Thread(new Runnable() {
            @Override
            public void run() {
                clientCommunication.clientConnect(clientCommunication, opcua_client, serverPath);
            }
        }).start();

        changeState(INITIATING);
        return true;
    }

    private void initiating() {

        //monitor remote endpoint
        //calling RequestLoading or unloading
        // TODO Auto-generated method stub
        changeState(INITIATED);
    }

    /**
     *
     */
    private void initiated() {
        // anay local conditions fulfilled

        changeState(READY);

    }

    private void ready() {

        // WAITING FOR REMOTE STATE TO CHANGE to READY
        //CALL REQUEST START LODING TO UNLOADING

        changeState(EXECUTE);

    }

    private void execute() {
        //EXECUTE
    }

    private void completing() {
        changeState(COMPLETED);

    }

    private void compeleted() {
        changeState(STOPPING);
    }


    private void stopping() {
        changeState(STOPPED);
    }

    private void stopped() {

    }

    private void changeState(ClientLoadingStates states) {

        switch (states) {
            case IDLE:
                currentState = IDLE.ordinal();
                idle();
                break;
            case STARTING:
                currentState = STARTING.ordinal();
                starting();
                break;
            case INITIATING:
                currentState = INITIATING.ordinal();
                initiating();
                break;
            case INITIATED:
                currentState = INITIATED.ordinal();
                initiated();
                break;
            case READY:
                currentState = READY.ordinal();
                ready();
                break;

            case EXECUTE:
                currentState = EXECUTE.ordinal();
                execute();
                break;
            case COMPLETING:
                currentState = COMPLETING.ordinal();
                completing();
                break;
            case COMPLETED:
                currentState = COMPLETED.ordinal();
                compeleted();
                break;
            case STOPPING:
                currentState = STOPPING.ordinal();
                stopping();
                break;
            case STOPPED:
                currentState = STOPPED.ordinal();
                stopped();
                break;
            default:
                // DONE needs to be called from inside this class
                // Invalid input should not be handled
                break;
        }
    }

    public void setServerPath(String serverPath) {
        this.serverPath = serverPath;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public void complete() {
        if (getCurrentState() == EXECUTE.ordinal()) {
            changeState(COMPLETING);
        }
    }

    public void reset() {
        if (getCurrentState() == STOPPED.ordinal()) {
            changeState(IDLE);
        }
    }

    public void start() {
        if (getCurrentState() == IDLE.ordinal()) {
            changeState(STARTING);
        }
    }
}
