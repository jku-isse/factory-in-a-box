package capabilities;

import communication.Communication;
import communication.open62communication.ServerCommunication;
import helper.CapabilityId;
import helper.CapabilityRole;
import helper.CapabilityType;

import java.util.List;

public class Endpoint {
    Capability defination;
    List<Capability> capabilities;
    List<Endpoint> endpoints;


    // private ServerCommunication serverCommunication;
    private Object opcua_server;
    private Object endpoint_NodeId;

    public Object getEndpoint_object() {
        return endpoint_object;
    }

    private Object endpoint_object;

    public Endpoint(String host, int port, String name, CapabilityId capabilityId, CapabilityType capabilityType, CapabilityRole capabilityRole) {
        Communication opcua_comm = new Communication();
        opcua_server = opcua_comm.getServerCommunication().createServer(host, port);
        endpoint_NodeId = opcua_comm.getServerCommunication().createNodeNumeric(1, 1000); //need to implement a controller level Enum
        if (name.isEmpty()) name = "END_POINT";
        endpoint_object = opcua_comm.getServerCommunication().addObject(opcua_server, endpoint_NodeId, name);


        defination = new Capability(opcua_comm.getServerCommunication(), opcua_server, endpoint_object,  capabilityId, capabilityType, capabilityRole);
    }

    public Endpoint(ServerCommunication serverCommunication, Object opcua_server, Object parentObjectId,  String name, CapabilityId capabilityId, CapabilityType capabilityType, CapabilityRole capabilityRole) {
        endpoint_NodeId = serverCommunication.createNodeNumeric(1, 66); //need to implement a controller level Enum

        endpoint_object = serverCommunication.addNestedObject(opcua_server, parentObjectId, endpoint_NodeId, name);

        defination = new Capability(serverCommunication, opcua_server, endpoint_object,  capabilityId, capabilityType, capabilityRole);
    }
}
