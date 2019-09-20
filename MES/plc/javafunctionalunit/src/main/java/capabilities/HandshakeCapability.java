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

import static helper.HandshakeStates.*;

import static helper.HandshakeStates.STOPPED;
import static helper.HandshakeStates.STOPPING;

import java.util.HashMap;
import java.util.Map;

import communication.Communication;
import communication.open62communication.ServerCommunication;
import communication.utils.RequestedNodePair;
import helper.*;
import protocols.LoadingClientProtocol;
import protocols.LoadingServerProtocol;

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

    public HandshakeCapability(ServerCommunication serverCommunication, Object server, Object parentObject, CapabilityId capabilityId, CapabilityRole capabilityRole) {
        super(serverCommunication, server, parentObject, capabilityId, CapabilityType.HANDSHAKE, capabilityRole);

        clientProtocol = null;
        serverProtocol = null;

        serverCommunication.addStringMethod(serverCommunication, server, parentObject, new RequestedNodePair<>(1, serverCommunication.getUnique_id()), "START",
                opcuaMethodInput -> {
                    return start(opcuaMethodInput);
                });
        serverCommunication.addStringMethod(serverCommunication, server, parentObject, new RequestedNodePair<>(1, serverCommunication.getUnique_id()), "STOP",
                opcuaMethodInput -> {
                    return stop(opcuaMethodInput);
                });
        serverCommunication.addStringMethod(serverCommunication, server, parentObject, new RequestedNodePair<>(1, serverCommunication.getUnique_id()), "RESET",
                opcuaMethodInput -> {
                    return reset(opcuaMethodInput);
                });


        serverCommunication.addStringMethod(serverCommunication, server, parentObject, new RequestedNodePair<>(1, serverCommunication.getUnique_id()), "INIT_Loading",
                opcuaMethodInput -> {
                    return initiateLoading(opcuaMethodInput);
                });
        serverCommunication.addStringMethod(serverCommunication, server, parentObject, new RequestedNodePair<>(1, serverCommunication.getUnique_id()), "INIT_Unloading",
                opcuaMethodInput -> {
                    return initiateUnloading(opcuaMethodInput);
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
        if (loadingMechanism == 1) {
        //    clientProtocol.fireTrigger(ClientLoadingStates.STARTING);
        }
        return "Start Complete";
    }
    public String stop(String inputPram) {
        if (loadingMechanism == 1) {
          //  clientProtocol.fireTrigger(ClientLoadingStates.STARTING);
        }
        return "Stop Complete";
    }
    public String reset(String inputPram) {
        if (loadingMechanism == 1) {
         //   clientProtocol.fireTrigger(ClientLoadingStates.STARTING);
        }
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
        return "initiateLoading was Successful";
    }

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
