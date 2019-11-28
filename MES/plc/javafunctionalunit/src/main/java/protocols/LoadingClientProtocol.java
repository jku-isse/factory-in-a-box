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


import capabilities.WiringInformation;
import communication.Communication;
import communication.open62communication.ClientCommunication;
import communication.utils.RequestedNodePair;
import helper.ClientLoadingStates;
import communication.utils.Pair;

import static helper.ClientLoadingStates.*;

public class LoadingClientProtocol {
    private int currentState;
    private WiringInformation currentWiringInformation;
    private String orderId;

    private Object opcua_client;
    private ClientCommunication clientCommunication;

    public LoadingClientProtocol(Communication communication, Object opcua_server, Object client, Object parentObject) {
        this.clientCommunication = communication.getClientCommunication();
        this.opcua_client = client;
        currentWiringInformation = null;

        communication.getServerCommunication().addStringMethod(communication.getServerCommunication(), opcua_server, parentObject, new RequestedNodePair<>(1, communication.getServerCommunication().getUnique_id()), "STOP",
                opcuaMethodInput -> {
                    return stop(opcuaMethodInput);
                });
        communication.getServerCommunication().addStringMethod(communication.getServerCommunication(), opcua_server, parentObject, new RequestedNodePair<>(1, communication.getServerCommunication().getUnique_id()), "RESET",
                opcuaMethodInput -> {
                    return reset(opcuaMethodInput);
                });
        communication.getServerCommunication().addStringMethod(communication.getServerCommunication(), opcua_server, parentObject, new RequestedNodePair<>(1, communication.getServerCommunication().getUnique_id()), "READY",
                opcuaMethodInput -> {
                    return ready();
                });
        communication.getServerCommunication().addStringMethod(communication.getServerCommunication(), opcua_server, parentObject, new RequestedNodePair<>(1, communication.getServerCommunication().getUnique_id()), "COMPLETE",
                opcuaMethodInput -> {
                    return complete(opcuaMethodInput);
                });
    }

    public final int getCurrentState() {
        return currentState;
    }

    public void idle() {
        System.out.println("Loading Client Protocol change State to IDLE");

        // comm.getClientCommunication().ClientSubtoNode(jClientAPIBase, client, nodeID)
    }

    public boolean starting(WiringInformation wiringInfo) {
        System.out.println("Loading Client Protocol change State to STARTING");

        changeState(STARTING);
        this.currentWiringInformation = wiringInfo;
        // int connection_states;
        // if (opcua_client == null || wiringInfo.getrEMOTE_ENDPOINT().isEmpty())
        //      return false;


        new Thread(new Runnable() {
            @Override
            public void run() {
                //  clientCommunication.clientConnect(clientCommunication, opcua_client, currentWiringInformation.getrEMOTE_ENDPOINT());
                //TODO: wait for connected event before changing the state
            }
        }).start();

        //  clientCommunication.clientSubToNode(clientCommunication, opcua_client, this.serverPath);
        changeState(INITIATING);
        return true;
    }

    private void initiating() {
        System.out.println("Loading Client Protocol change State to INITIATING");


        //monitor remote endpoint
        //calling RequestLoading or unloading
        new Thread(new Runnable() {
            @Override
            public void run() {
                String callbCK = "CALL BACK";
                if (currentWiringInformation.getrEMOTE_ROLE().contains("PROVIDER"))
                    callbCK = clientCommunication.callStringMethod(currentWiringInformation.getrEMOTE_ENDPOINT(), new Pair<>(Integer.parseInt(currentWiringInformation.getRemote_NODEID_NameSpace().trim()), currentWiringInformation.getRemote_NODEID_STRINGID()), new Pair<>(1, "REQUEST_INIT_HANDOVER"), orderId);
                else
                    callbCK = clientCommunication.callStringMethod(currentWiringInformation.getrEMOTE_ENDPOINT(), new Pair<>(Integer.parseInt(currentWiringInformation.getRemote_NODEID_NameSpace().trim()), currentWiringInformation.getRemote_NODEID_STRINGID()), new Pair<>(1, "REQUEST_INIT_UNLOADING"), orderId);

                System.out.println(callbCK);
//TODO: CHECK THE RETURN THEN ACCORDINGLY WAIT OR MOVE TO ANOTHER STATE

                changeState(INITIATED);
            }
        }).start();
        // changeState(INITIATED);
    }

    /**
     *
     */
    private void initiated() {
        System.out.println("Loading Client Protocol change State to INITIATED");


        // anay local conditions fulfilled

        changeState(READY);

    }

    private String ready() {
        System.out.println("Loading Client Protocol change State to READY");


        new Thread(new Runnable() {
            @Override
            public void run() {
                String callbCK = "CALL BACK";
                if (currentWiringInformation.getrEMOTE_ROLE().contains("PROVIDER"))
                    callbCK = clientCommunication.callStringMethod(currentWiringInformation.getrEMOTE_ENDPOINT(), new Pair<>(Integer.parseInt(currentWiringInformation.getRemote_NODEID_NameSpace().trim()), currentWiringInformation.getRemote_NODEID_STRINGID()), new Pair<>(1, "REQUEST_START_HANDOVER"), orderId);
                else
                    callbCK = clientCommunication.callStringMethod(currentWiringInformation.getrEMOTE_ENDPOINT(), new Pair<>(Integer.parseInt(currentWiringInformation.getRemote_NODEID_NameSpace().trim()), currentWiringInformation.getRemote_NODEID_STRINGID()), new Pair<>(1, "REQUEST_START_UNLOADING"), orderId);

                System.out.println(callbCK);
//TODO: CHECK THE RETURN THEN ACCORDINGLY WAIT OR MOVE TO ANOTHER STATE

            }
        }).start();

        changeState(EXECUTE);
        return "Ready State Complete";
    }

    private void execute() {
        System.out.println("Loading Client Protocol change State to EXECUTE");


        //EXECUTE
    }

    private void completing() {
        System.out.println("Loading Client Protocol change State to COMPLETING");


        changeState(COMPLETED);

    }

    private void compeleted() {
        System.out.println("Loading Client Protocol change State to COMPLETED");


        changeState(STOPPING);
    }


    private void stopping() {
        System.out.println("Loading Client Protocol change State to STOPPING");


        changeState(STOPPED);
    }

    private void stopped() {
        System.out.println("Loading Client Protocol change State to STOPPED");


    }

    private void changeState(ClientLoadingStates states) {

        switch (states) {
            case IDLE:
                currentState = IDLE.ordinal();
                idle();
                break;
            case STARTING:
                currentState = STARTING.ordinal();

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


    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String complete(String opcuaInput) {
        System.out.println("Loading Client Protocol OPCUA CALL to Complete");


        if (getCurrentState() == EXECUTE.ordinal()) {
            changeState(COMPLETING);
            return "Complete Successful";
        } else return "WRONG STATE NOT @ EXECUTE";
    }

    public String reset(String opcuaInput) {
        System.out.println("Loading Client Protocol OPCUA CALL to Reset");


        if (getCurrentState() == STOPPED.ordinal()) {
            changeState(IDLE);
            return "reset Successful";
        } else return "WRONG STATE NOT @ STOPPED";
    }

    /*
    public String start(String opcuaInput) {
        if (getCurrentState() == IDLE.ordinal()) {
            //    if (!starting())
            //    return "ERROR WHEN @ STARTING";
            changeState(STARTING);
            return "STARTING Successful";
        } else return "WRONG STATE NOT @ IDLE";
    }
*/
    public String stop(String opcuaInput) {
        System.out.println("Loading Client Protocol OPCUA CALL to Stop");


        changeState(STOPPING);
        return "Stop Complete";
    }
}
