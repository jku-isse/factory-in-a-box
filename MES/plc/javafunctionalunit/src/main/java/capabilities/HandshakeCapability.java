/**
 * [Class description.  The first sentence should be a meaningful summary of the class since it
 * will be displayed as the class summary on the Javadoc package page.]
 * <p>
 * [Other notes, including guaranteed invariants, usage instructions and/or examples, reminders
 * about desired improvements, etc.]
 *
 * @author Michael Bishara
 * @author <A HREF="mailto:[michaelbishara14@gmail.com]">[Michael Bishara]</A>
 * @author <A HREF="https://github.com/michaelanis14">[Github: Michael Bishara]</A>
 * @date 4 Sep 2019
 **/
package capabilities;

import communication.open62communication.ClientCommunication;
import communication.open62communication.ServerCommunication;
import communication.utils.RequestedNodePair;
import helper.CapabilityId;
import helper.CapabilityRole;
import helper.CapabilityType;
import helper.HandshakeStates;
import protocols.LoadingClientProtocol;
import protocols.LoadingServerProtocol;

import static helper.HandshakeStates.*;


class startHandshakeEvent extends CapabilityEvent {
    public startHandshakeEvent(HandshakeCapability source) {  super(source); }
}

class stopHandshakeEvent extends CapabilityEvent {
    public stopHandshakeEvent(HandshakeCapability source) {
        super(source);
    }
}

class resetHandshakeEvent extends CapabilityEvent {
    public resetHandshakeEvent(HandshakeCapability source) {
        super(source);
    }
}

class initLoadingHandshakeEvent extends CapabilityEvent {
    public initLoadingHandshakeEvent(HandshakeCapability source) {
        super(source);
    }
}

class initUnloadingHandshakeEvent extends CapabilityEvent {
    public initUnloadingHandshakeEvent(HandshakeCapability source) {
        super(source);
    }
}


public class HandshakeCapability extends Capability {

    public final static boolean DEBUG = true;
    // Map<CapabilityInstanceId, Protocol> protocolMap;
    LoadingClientProtocol clientProtocol;
    LoadingServerProtocol serverProtocol;
    int loadingMechanism;

    // opcua

    public void setWiring() {

        System.out.println("Method Callback stop");

    }

    public void initLoading() {

        System.out.println("Method Callback stop");

    }

    public void initUnloading() {

        System.out.println("Method Callback stop");

    }

    public HandshakeCapability(ClientCommunication clientCommunication, Object client, Object parentObject, CapabilityId capabilityId) {
        super(clientCommunication, client, parentObject, capabilityId, CapabilityType.HANDSHAKE, CapabilityRole.Required);

        clientProtocol = null;
        serverProtocol = null;
    }


    public HandshakeCapability(ServerCommunication serverCommunication, Object server, Object parentObject, CapabilityId capabilityId) {
        super(serverCommunication, server, parentObject, capabilityId, CapabilityType.HANDSHAKE, CapabilityRole.Provided);
        // super(serverCommunication, server, parentObject, capabilityId);

        clientProtocol = null;
        serverProtocol = null;

        serverCommunication.addStringMethod(serverCommunication, server, parentObject, new RequestedNodePair<>(1, serverCommunication.getUnique_id()), "START",
                opcuaMethodInput -> {
                    return start(opcuaMethodInput);
                });
        serverCommunication.addStringMethod(serverCommunication, server, parentObject, new RequestedNodePair<>(1, serverCommunication.getUnique_id()), "STOP",
                opcuaMethodInput -> {
                    return stop( opcuaMethodInput);
                });
        serverCommunication.addStringMethod(serverCommunication, server, parentObject, new RequestedNodePair<>(1, serverCommunication.getUnique_id()), "RESET",
                opcuaMethodInput -> {
                    return reset( opcuaMethodInput);
                });


        serverCommunication.addStringMethod(serverCommunication, server, parentObject, new RequestedNodePair<>(1, serverCommunication.getUnique_id()), "INIT_LOADING",
                opcuaMethodInput -> {
                    return initiateLoading((String) opcuaMethodInput);
                });
        serverCommunication.addStringMethod(serverCommunication, server, parentObject, new RequestedNodePair<>(1, serverCommunication.getUnique_id()), "INIT_UNLOADING",
                opcuaMethodInput -> {
                    return initiateUnloading((String) opcuaMethodInput);
                });


        fireTrigger(IDLE);
    }

    static int currentState;
    LoadingClientProtocol EngageInUnLoading;

    public void fireTrigger(HandshakeStates states) {

        switch (states) {
            case IDLE:
                currentState = IDLE.ordinal();
                idle();
                break;
            case STARTING:
                currentState = STARTING.ordinal();
                start("");
                break;
            case EXECUTE:
                currentState = EXECUTE.ordinal();
                execute();
                break;
            case COMPLETING:
                currentState = COMPLETING.ordinal();
                completing();
                break;
            case COMPLETE:
                currentState = COMPLETE.ordinal();
                compelete();
                break;

            case RESETTING:
                currentState = RESETTING.ordinal();
                resetting();
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

    public final int getCurrentState() {
        return currentState;
    }

    public void idle() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //       opcua_comm.getServerCommunication().runServer(opcua_server);
            }
        }).start();
    }

    public String start(String inputPram) {
        if (this.getCapabilityRole().compareTo(CapabilityRole.Provided) == 0) {
            //  int inputArray[] = {5, 6, 7};
            // System.out.println( "ARAAAYY "+this.getClientCommunication().callArrayMethod("opc.tcp://localhost:4840", new RequestedNodePair<>(1, 66), new RequestedNodePair<>(1, 18),
            //             inputArray)) ;
            //    clientProtocol.fireTrigger(ClientLoadingStates.STARTING);
        }

        fireEvent(new startHandshakeEvent(this));
        return "Start Complete";
    }

    public String stop(String inputPram) {
        if (loadingMechanism == 1) {
            //  clientProtocol.fireTrigger(ClientLoadingStates.STARTING);
        }
        fireEvent(new stopHandshakeEvent(this));
        return "Stop Complete";
    }

    public String reset(String inputPram) {
        if (loadingMechanism == 1) {
            //   clientProtocol.fireTrigger(ClientLoadingStates.STARTING);
        }
        fireEvent(new resetHandshakeEvent(this));
        return "Reset Complete";
    }

    public void execute() {

    }

    public void completing() {

    }

    public void compelete() {

    }

    public void resetting() {

    }

    public void stopping() {

    }

    public void stopped() {

    }

    //Set initiateUnloading Method has one string input from opcua callback thus
    // all the needed params are ';' separated
    // the first input is CapabilityID followed by orderId
    // the CapabilityID should match the Enum attributes found in helper/CapabilityId
    public String initiateUnloading(String inputPram) {
        String[] inputParamters = inputPram.split(";");
        System.out.println(inputParamters.toString());
        if (inputParamters.length == 1)
            return "Wrong Parameters, Please separate the CabilityID and OrderID with ';'";
        else {
            try {
                CapabilityId localCapabilityId = CapabilityId.valueOf(inputParamters[0]);
                String orderId = inputParamters[1];
                //   this.wiringMap.put(localCapabilityId, orderId);
            } catch (IllegalArgumentException e) {
                return "Wrong Parameters, Could not Match CabilityID";
            }
        }
        fireEvent(new initUnloadingHandshakeEvent(this));
        return "initiateLoading was Successful";
    }

    //Set initiateUnloading Method has one string input from opcua callback thus
    // all the needed params are ';' separated
    // the first input is CapabilityID followed by orderId
    // the CapabilityID should match the Enum attributes found in helper/CapabilityId

    public String initiateLoading(String inputPram) {
        String[] inputParamters = inputPram.split(";");
        System.out.println(inputParamters.toString());
        if (inputParamters.length == 1)
            return "Wrong Parameters, Please separate the CabilityID and OrderID with ';'";
        else {
            try {
                CapabilityId localCapabilityId = CapabilityId.valueOf(inputParamters[0]);
                String orderId = inputParamters[1];
                //   this.wiringMap.put(localCapabilityId, orderId);
            } catch (IllegalArgumentException e) {
                return "Wrong Parameters, Could not Match CabilityID";
            }
        }
        fireEvent(new initLoadingHandshakeEvent(this));
        return "initiateLoading was Successful";
    /*    //(CapabilityId instanceId, String orderId) {
        loadingMechanism = 1;
        // this.protocolMap.get(instanceId)
        if (this.getCurrentState() != IDLE.ordinal()) {
            log("Not Idle - Wrong State. !");
            return "";
        }

        //   clientProtocol.setServerPath(this.wiringMap.get(instanceId));
        //   clientProtocol.setOrderId(orderId);
        this.fireTrigger(STARTING);

        return "";

     */
    }

    public void setRequiredCapability(CapabilityId instanceId, CapabilityType typeId) {
        //      this.capabilityMap.put(instanceId, typeId);
        // this.protocolMap.put(instanceId, initCapability(instanceId));
        initCapability(instanceId);
    }


    public void initCapability(CapabilityId instanceId) {
        /*
         * } if (instanceId.toString().contains("SERVER")) return new
         * LoadingServerProtocol(); else return new LoadingClientProtocol();
         */
        if (instanceId.toString().contains("SERVER") && serverProtocol == null) {
            serverProtocol = new LoadingServerProtocol();

        } else if (clientProtocol == null) {
            clientProtocol = new LoadingClientProtocol();
        }
    }

    // Loging method to catpture printouts if the Debug flag is set
    //TODO shall be moved to a controller class
    public static void log(String message) {
        if (DEBUG) {
            String fullClassName = Thread.currentThread().getStackTrace()[2].getClassName();
            String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
            String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
            int lineNumber = Thread.currentThread().getStackTrace()[2].getLineNumber();

            System.out.println(className + "." + methodName + "(): " + lineNumber + "  " + message);
        }
    }


}
