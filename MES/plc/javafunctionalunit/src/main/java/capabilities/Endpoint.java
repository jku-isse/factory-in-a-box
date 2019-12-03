package capabilities;

import communication.Communication;
import communication.open62communication.ClientCommunication;
import communication.open62communication.ServerCommunication;
import helper.CapabilityId;
import helper.CapabilityRole;
import helper.CapabilityType;

import java.util.ArrayList;
import java.util.List;

public class Endpoint {

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
        capabilities = new ArrayList<>();

        Communication opcua_comm = new Communication();
        opcua_server = opcua_comm.getServerCommunication().createServer(host, port);

        if (name.isEmpty()) name = "END_POINT";
        String endpointStringNodeId = ("END_POINT_"+capabilityType.toString()+"_")+capabilityId.toString()+"_"+capabilityRole.toString();

        endpoint_NodeId = opcua_comm.getServerCommunication().createNodeString(1, endpointStringNodeId); //need to implement a controller level Enum

        endpoint_object = opcua_comm.getServerCommunication().addObject(opcua_server, endpoint_NodeId, name);


        // Capability  defination = new Capability(opcua_comm.getServerCommunication(), opcua_server, endpoint_object,  capabilityId, capabilityType, capabilityRole);
        //capabilities.add(defination);
    }

    public Endpoint(ServerCommunication serverCommunication, Object opcua_server, Object parentObjectId, String name, CapabilityType capabilityType,CapabilityId capabilityId, CapabilityRole capabilityRole) {
        capabilities = new ArrayList<>();

        if (name.isEmpty()) name = "END_POINT";
        String endpointStringNodeId = ("END_POINT_"+capabilityType.toString()+"_")+capabilityRole.toString()+"_"+capabilityId.toString()+"_"+name;

        endpoint_NodeId = serverCommunication.createNodeString(1, endpointStringNodeId); //need to implement a controller level Enum

        endpoint_object = serverCommunication.addNestedObject(opcua_server, parentObjectId, endpoint_NodeId, name);

        //   Capability defination = new Capability(serverCommunication, opcua_server, endpoint_object,  capabilityId, capabilityType, capabilityRole);
        //  capabilities.add(defination);
    }
    public Endpoint(ClientCommunication clientCommunication, Object opcua_client, Object parentObjectId, String name, CapabilityId capabilityId, CapabilityType capabilityType, CapabilityRole capabilityRole) {
        capabilities = new ArrayList<>();

        //   Capability defination = new Capability(serverCommunication, opcua_server, endpoint_object,  capabilityId, capabilityType, capabilityRole);
        //  capabilities.add(defination);
    }
}
