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

import communication.Communication;
import communication.open62communication.ServerCommunication;
import communication.utils.Pair;
import helper.CapabilityId;
import helper.CapabilityRole;
import helper.CapabilityType;
import helper.HandshakeStates;
import lombok.Getter;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import protocols.LoadingClientProtocol;
import protocols.LoadingServerProtocol;

import static helper.HandshakeStates.*;


class startHandshakeEvent extends CapabilityEvent {
    public startHandshakeEvent(HandshakeCapability source) {
        super(source);
    }
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
    Object state_nodeid;

    @Getter
    Object endpoint_NodeId;
    @Getter
    Object capabilities_NodeId;

    public CapabilityId getCurrentCapabilityId() {
        return currentCapabilityId;
    }

    public String getCurrentOrderId() {
        return currentOrderId;
    }

    CapabilityId currentCapabilityId;
    String currentOrderId;
    // opcua


    public void initLoading() {

        System.out.println("Method Callback stop");

    }

    public void initUnloading() {

        System.out.println("Method Callback stop");

    }

    public HandshakeCapability(Communication communication, Object opcua_server, Object client, Object parentObject, Object capabilitiesObject, CapabilityId capabilityId) {
        super(communication, opcua_server, client, capabilitiesObject, capabilityId, CapabilityType.HANDSHAKE, CapabilityRole.Required);

        this.endpoint_NodeId = parentObject;
        this.capabilities_NodeId = capabilitiesObject;

        String nodePrefix = ("CAPABILITY_" + CapabilityType.HANDSHAKE.toString() + "_") + capabilityId.toString() + "_";
        clientProtocol = new LoadingClientProtocol(communication, opcua_server, client, parentObject, nodePrefix);
        serverProtocol = null;

        //ROLE
        communication.getServerCommunication().addStringMethod(communication.getServerCommunication(), opcua_server, parentObject, new Pair<>(1, nodePrefix + "START"), "START",
                opcuaMethodInput -> {
                    return start(opcuaMethodInput); //MOVE START  TO PROTOCOL
                });

/*
        communication.getServerCommunication().addStringMethod(communication.getServerCommunication(), opcua_server, parentObject, new RequestedNodePair<>(1, communication.getServerCommunication().getUnique_id()), "INIT_LOADING",
                opcuaMethodInput -> {
                    return initiateLoading(opcuaMethodInput);
                });
        communication.getServerCommunication().addStringMethod(communication.getServerCommunication(), opcua_server, parentObject, new RequestedNodePair<>(1, communication.getServerCommunication().getUnique_id()), "INIT_UNLOADING",
                opcuaMethodInput -> {
                    return initiateUnloading(opcuaMethodInput);
                });

*/
        state_nodeid = communication.getServerCommunication().addStringVariableNode(opcua_server, parentObject, new Pair<>(1, nodePrefix + "STATE"), "STATE"); //MOVE STATE TO PROTOCOL

        changeState(IDLE);

    }


    public HandshakeCapability(ServerCommunication serverCommunication, Object server, Object parentObject, Object capabilitiesObject, CapabilityId capabilityId) {
        super(serverCommunication, server, capabilitiesObject, capabilityId, CapabilityType.HANDSHAKE, CapabilityRole.Provided);
        // super(serverCommunication, server, parentObject, capabilityId);
        this.endpoint_NodeId = parentObject;
        this.capabilities_NodeId = capabilitiesObject;

        String nodePrefix = ("CAPABILITY_" + CapabilityType.HANDSHAKE.toString() + "_") + capabilityId.toString() + "_";
        clientProtocol = null;
        serverProtocol = new LoadingServerProtocol(serverCommunication, server, parentObject, nodePrefix);


        // state_nodeid = serverCommunication.addStringVariableNode(server, parentObject, new Pair<>(1, nodePrefix+"_"+"STATE"), "STATE");
        //   communication.getServerCommunication().writeVariable(opcua_server, state_nodeid, "IDLE");


        changeState(IDLE);
    }

    static int currentState;
    LoadingClientProtocol EngageInUnLoading;

    public void changeState(HandshakeStates states) {

        switch (states) {
            case IDLE:
                currentState = IDLE.ordinal();
                idle();
                break;
            case STARTING:
                currentState = STARTING.ordinal();
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
        //if (serverProtocol != null)
        //   getServerCommunication().writeVariable(getServer(), state_nodeid, HandshakeStates.values()[currentState].toString());
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

    public String starting(WiringInformation wiringInformation, String orderId) {
        changeState(STARTING);

        if (wiringInformation.getlOCAL_CAPABILITYID().toString().contains("SERVER")) {
            clientProtocol.setOrderId(orderId);

            clientProtocol.reset("");
            clientProtocol.starting(wiringInformation);
        }


        //fireEvent(new startHandshakeEvent(this));
        return "Start Complete";
    }

    public String ready(String inputPram) {
        if (clientProtocol != null) {
        }
        fireEvent(new stopHandshakeEvent(this));
        return "Stop Complete";
    }

    public String stop(String inputPram) {
        if (clientProtocol != null) {
        }
        fireEvent(new stopHandshakeEvent(this));
        return "Stop Complete";
    }

    public String reset(String inputPram) {
        if (clientProtocol != null) {

        }
        fireEvent(new resetHandshakeEvent(this));
        return "Reset Complete";
    }

    public void execute() {
        changeState(EXECUTE);

        if (getCurrentCapabilityId().toString().contains("SERVER")) {
            //    clientProtocol.changeState(ClientLoadingStates.EXECUTE);
        }
    }

    public void completing() {

    }

    public String compelete() {
        return "Complete completed";
    }

    public void resetting() {

    }

    public void stopping() {

    }

    public void stopped() {

    }

    //Set initiateUnloading Method has one string input from opcua callback thus
    // all the needed params should be in Json format
    // the first input is CapabilityID followed by orderId
    // the CapabilityID should match the Enum attributes found in helper/CapabilityId
    public String initiateUnloading(String inputPram) {
        if (clientProtocol != null) {
        }
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
        return "initiateUnloading was Successful";
    }

    //Set initiateUnloading Method has one string input from opcua callback thus
    // all the needed params should be in Json format
    // the first input is CapabilityID followed by orderId
    // the CapabilityID should match the Enum attributes found in helper/CapabilityId

    public String start(String inputPram) {

        //    if (clientProtocol != null) {
        //   }
        JSONObject loadingJson;
        currentOrderId = "-1";
        try {
            Object obj = new JSONParser().parse(inputPram);
            loadingJson = (JSONObject) obj;
        } catch (ParseException e) {
            e.printStackTrace();
            return "Error at parsing Json input";
        }

        String LOCAL_CAPABILITYID_String = (String) loadingJson.get("LOCAL_CAPABILITYID");
        String ORDER_ID_String = (String) loadingJson.get("ORDER_ID");
        if (LOCAL_CAPABILITYID_String == null || ORDER_ID_String == null || LOCAL_CAPABILITYID_String.isEmpty() || ORDER_ID_String.isEmpty())
            return "Wrong Parameters!  ";

        if (this.getCurrentState() != IDLE.ordinal()) {
            return ("Not Idle - Wrong State. !");
        }
        currentCapabilityId = CapabilityId.valueOf(LOCAL_CAPABILITYID_String);
        currentOrderId = ORDER_ID_String;


        ///this.fireTrigger(STARTING);


        fireEvent(new startHandshakeEvent(this));
        return "Initiate_Loading was Successful";
    }


}
