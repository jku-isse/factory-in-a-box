package capabilities;


import communication.open62communication.ServerCommunication;
import communication.utils.RequestedNodePair;
import helper.CapabilityId;
import helper.CapabilityRole;
import helper.CapabilityType;


import java.util.HashMap;
import java.util.Map;

public class WiringCapability extends Capability {


    Map<CapabilityId, String> wiringMap;


    public WiringCapability(ServerCommunication serverCommunication, Object opcua_server, Object parentObject, CapabilityId capabilityId, CapabilityRole capabilityRole) {
        super(serverCommunication, opcua_server, parentObject, capabilityId, CapabilityType.WIRING, capabilityRole);
        wiringMap = new HashMap<CapabilityId, String>();

        serverCommunication.addStringMethod(serverCommunication, opcua_server, parentObject, new RequestedNodePair<>(1, serverCommunication.getUnique_id()), "SET_WIRING",
                opcuaMethodInput -> {
                    return setWiring(opcuaMethodInput); // the opcua method callback is received here
                });


        Object wiring_NodeId = serverCommunication.createNodeNumeric(1, 1000); //need to implement a controller level Enum
        Object wiring_object = serverCommunication.addNestedObject(opcua_server,this.getCapabilityObject(), wiring_NodeId, "CAPABILITY_WIRING");

        Object remoteEndpoint_nodeid = serverCommunication.addStringVariableNode(opcua_server, wiring_object, new RequestedNodePair<>(1, serverCommunication.getUnique_id()), "REMOTE_ENDPOINT");
        serverCommunication.writeVariable(opcua_server, remoteEndpoint_nodeid, "-");

        Object remoteNodeId_nodeid = serverCommunication.addStringVariableNode(opcua_server, wiring_object, new RequestedNodePair<>(1, serverCommunication.getUnique_id()), "REMOTE_NODEID");
        serverCommunication.writeVariable(opcua_server, remoteNodeId_nodeid, "-");

        Object remoteRole_nodeid = serverCommunication.addStringVariableNode(opcua_server, wiring_object, new RequestedNodePair<>(1, serverCommunication.getUnique_id()), "REMOTE_ROLE");
        serverCommunication.writeVariable(opcua_server, remoteRole_nodeid, "-");


    }


    //Set Wiring Method has one string input from opcua callback thus
    // all the needed params are ';' separated
    // the first input is CapabilityID followed by remoteCapabiltyEntryPoint
    // the CapabilityID should match the Enum attributes found in helper/CapabilityId
    public String setWiring(String wiringInfo) { // Serveraddress+NodeID


        String[] wiringParamters = wiringInfo.split(";");
        //  System.out.println(wiringParamters.toString());
        if (wiringParamters.length == 1)
            return "Wrong Parameters, Please separate the CapabilityID and Path with ';'";
        else {
            try {
                CapabilityId localCapabilityId = CapabilityId.valueOf(wiringParamters[0]); //Comparing the first part of the string to the Enums of CapabilityId
                String remoteCapabiltyEntryPoint = wiringParamters[1]; //getting the path for this capability id, later this will be used by the client to connect to this server endpoint
                this.wiringMap.put(localCapabilityId, remoteCapabiltyEntryPoint);
            } catch (IllegalArgumentException e) {
                return "Wrong Parameters, Could not Match CapabilityID";
            }
        }
        return "Wiring was Successful";
        //
        //

    }


    public String getWiring(CapabilityId capabilityId) {
        if (wiringMap.containsKey(capabilityId))
            return wiringMap.get(capabilityId);
        else return "-1";
    }

    public void setWiringMap(CapabilityId capabilityId, String path) {
        this.wiringMap.put(capabilityId, path);
    }
}
