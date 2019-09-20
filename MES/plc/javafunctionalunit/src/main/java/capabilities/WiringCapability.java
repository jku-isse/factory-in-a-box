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


    public WiringCapability(ServerCommunication serverCommunication, Object server, Object parentObject, CapabilityId capabilityId, CapabilityRole capabilityRole) {
        super(serverCommunication, server, parentObject, capabilityId, CapabilityType.WIRING, capabilityRole);
        wiringMap = new HashMap<CapabilityId, String>();
        serverCommunication.addStringMethod(serverCommunication, server, parentObject, new RequestedNodePair<>(1, serverCommunication.getUnique_id()), "SET_Wiring",
                opcuaMethodInput -> {
                    return setWiring(opcuaMethodInput);
                });

    }


    public String setWiring(String wiringInfo) { // Serveraddress+NodeID

        String[] wiringParamters = wiringInfo.split(";");
        System.out.println(wiringParamters.toString());
        if (wiringParamters.length == 1)
            return "Wrong Parameters, Please separate the CabilityID and Path with ';'";
        else {
            try {
                CapabilityId localCapabilityId = CapabilityId.valueOf(wiringParamters[0]);
                String remoteCapabiltyEntryPoint = wiringParamters[1];
                this.wiringMap.put(localCapabilityId, remoteCapabiltyEntryPoint);
            } catch (IllegalArgumentException e) {
                return "Wrong Parameters, Could not Match CabilityID";
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
