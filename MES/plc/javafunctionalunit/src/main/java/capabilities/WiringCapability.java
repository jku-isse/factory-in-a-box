package capabilities;


import communication.open62communication.ServerCommunication;
import communication.utils.Pair;
import helper.CapabilityId;
import helper.CapabilityRole;
import helper.CapabilityType;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.HashMap;
import java.util.Map;


class WiringCapabilityEvent extends CapabilityEvent {
    public WiringCapabilityEvent(WiringCapability source) {
        super(source);
    }
}


public class WiringCapability extends Capability {


    Map<CapabilityId, WiringInformation> wiringMap;
    private Object remoteEndpoint_nodeid;
    private Object remoteNodeId_nodeid;
    private Object remoteRole_nodeid;
    private Object localCapabilityId_nodeid;

    private Object wiring_NodeId;
    private Object wiring_object;


    public WiringCapability(ServerCommunication serverCommunication, Object opcua_server, Object parentObject, CapabilityId capabilityId) {
        super(serverCommunication, opcua_server, parentObject, capabilityId, CapabilityType.WIRING, CapabilityRole.Provided);
        wiringMap = new HashMap<CapabilityId, WiringInformation>();

        serverCommunication.addStringMethod(serverCommunication, opcua_server, parentObject, new Pair<>(1,("CAPABILITY_"+CapabilityType.WIRING.toString()+"_")+capabilityId.toString()+"_"+"SET_WIRING"), "SET_WIRING",
                opcuaMethodInput -> {
                    return setWiringInfo(opcuaMethodInput); // the opcua method callback is received here

                });


        wiring_NodeId = serverCommunication.createNodeNumeric(1, 1000); //need to implement a controller level Enum
        wiring_object = serverCommunication.addNestedObject(opcua_server, this.getCapabilityObject(), wiring_NodeId, "CAPABILITY_WIRING");


        localCapabilityId_nodeid = serverCommunication.addStringVariableNode(opcua_server, wiring_object,new Pair<>(1,("CAPABILITY_"+CapabilityType.WIRING.toString()+"_")+capabilityId.toString()+"_"+"LOCAL_CAPABILITYID"), "LOCAL_CAPABILITYID");
        serverCommunication.writeVariable(opcua_server, localCapabilityId_nodeid, "-");

        remoteEndpoint_nodeid = serverCommunication.addStringVariableNode(opcua_server, wiring_object, new Pair<>(1,("CAPABILITY_"+CapabilityType.WIRING.toString()+"_")+capabilityId.toString()+"_"+"REMOTE_ENDPOINT"), "REMOTE_ENDPOINT");
        serverCommunication.writeVariable(opcua_server, remoteEndpoint_nodeid, "-");

        remoteNodeId_nodeid = serverCommunication.addStringVariableNode(opcua_server, wiring_object,new Pair<>(1,("CAPABILITY_"+CapabilityType.WIRING.toString()+"_")+capabilityId.toString()+"_"+"REMOTE_NODEID"), "REMOTE_NODEID");
        serverCommunication.writeVariable(opcua_server, remoteNodeId_nodeid, "-");

        remoteRole_nodeid = serverCommunication.addStringVariableNode(opcua_server, wiring_object, new Pair<>(1,("CAPABILITY_"+CapabilityType.WIRING.toString()+"_")+capabilityId.toString()+"_"+"REMOTE_ROLE"), "REMOTE_ROLE");
        serverCommunication.writeVariable(opcua_server, remoteRole_nodeid, "-");


    }


    //Set Wiring Method has one string input from opcua callback thus
    // all the needed params are ';' separated
    // the first input is CapabilityID followed by remoteCapabiltyEntryPoint
    // the CapabilityID should match the Enum attributes found in helper/CapabilityId

    public String setWiringInfo(String wiringInfo) {
        JSONObject wiringJson;
        try {
            Object obj = new JSONParser().parse(wiringInfo);
            wiringJson = (JSONObject) obj;
        } catch (ParseException e) {
            e.printStackTrace();
            return "Error at parsing Json input";
        }

        String LOCAL_CAPABILITYIDString = (String) wiringJson.get("LOCAL_CAPABILITYID");
        String REMOTE_ENDPOINTString = (String) wiringJson.get("REMOTE_ENDPOINT");
        String REMOTE_NODEIDString = (String) wiringJson.get("REMOTE_NODEID");
        String REMOTE_ROLEString = (String) wiringJson.get("REMOTE_ROLE");


        //  System.out.println(wiringParamters.toString());
        if (LOCAL_CAPABILITYIDString == null || REMOTE_ENDPOINTString == null || LOCAL_CAPABILITYIDString.isEmpty() || REMOTE_ENDPOINTString.isEmpty())
            return "Wrong Parameters!";
        else {
            try {
                CapabilityId localCapabilityId = CapabilityId.valueOf(LOCAL_CAPABILITYIDString); //Comparing the first part of the string to the Enums of CapabilityId
                //getting the path for this capability id, later this will be used by the client to connect to this server endpoint
                WiringInformation wiringInformation = new WiringInformation(LOCAL_CAPABILITYIDString, REMOTE_ENDPOINTString, REMOTE_NODEIDString, REMOTE_ROLEString);
                this.wiringMap.put(localCapabilityId, wiringInformation);

            } catch (IllegalArgumentException e) {
                return "Wrong Parameters, Could not Match CapabilityID";
            }
        }

        getServerCommunication().writeVariable(getServer(), localCapabilityId_nodeid, LOCAL_CAPABILITYIDString);
        getServerCommunication().writeVariable(getServer(), remoteEndpoint_nodeid, REMOTE_ENDPOINTString);
        getServerCommunication().writeVariable(getServer(), remoteNodeId_nodeid, REMOTE_NODEIDString);
        getServerCommunication().writeVariable(getServer(), remoteRole_nodeid, REMOTE_ROLEString);

        fireEvent(new WiringCapabilityEvent(this));


        return "Wiring was Successful";
    }


    public WiringInformation getWiring(CapabilityId capabilityId) {
        if (wiringMap.containsKey(capabilityId))
            return wiringMap.get(capabilityId);
        else return null;
    }




}
