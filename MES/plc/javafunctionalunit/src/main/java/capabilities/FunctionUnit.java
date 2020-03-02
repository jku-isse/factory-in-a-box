package capabilities;

import communication.open62communication.ServerCommunication;
import helper.CapabilityType;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class FunctionUnit {

    @Getter
    List<Capability> capabilities;
    @Getter
    List<FunctionUnit> functionUnits;

    private Object opcua_server;
    private Object functionUnit_NodeId;

    @Getter
    private Object functionUnit_object;

    public FunctionUnit(ServerCommunication serverCommunication, Object opcua_server, Object parentObjectId, String name, CapabilityType capabilityType) {
        capabilities = new ArrayList<>();
        if (name.isEmpty()) name = "END_POINT";
        String endpointStringNodeId = ("END_POINT_" + capabilityType.toString()) + "_" + name;
        functionUnit_NodeId = serverCommunication.createNodeString(1, endpointStringNodeId); //need to implement a controller level Enum
        functionUnit_object = serverCommunication.addNestedObject(opcua_server, parentObjectId, functionUnit_NodeId, name);

    }
}
