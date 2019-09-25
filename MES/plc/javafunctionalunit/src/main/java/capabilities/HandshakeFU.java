package capabilities;

import capabilities.Capability;
import communication.Communication;
import communication.open62communication.ServerCommunication;
import communication.utils.RequestedNodePair;
import helper.CapabilityId;
import helper.CapabilityRole;
import helper.CapabilityType;

import java.util.List;

public class HandshakeFU extends Endpoint {


    HandshakeCapability handshake;
    WiringCapability wiring;

    public HandshakeFU(ServerCommunication serverCommunication, Object opcua_server, Object parentObjectId, CapabilityId capabilityId , CapabilityRole capabilityRole) {
        //calling the Endpoint parent class constructor to init an opcua object to be the parent node for the sub-capabilities
        super(serverCommunication, opcua_server, parentObjectId,  CapabilityType.HANDSHAKE.toString()+"_FU", capabilityId, CapabilityType.HANDSHAKE, capabilityRole);

        Object state_nodeid = serverCommunication.addStringVariableNode(opcua_server, this.getEndpoint_object(), new RequestedNodePair<>(1, serverCommunication.getUnique_id()), "STATE");
        serverCommunication.writeVariable(opcua_server, state_nodeid, "IDLE");
       //initializing the main capabilities for the handshake FU
        handshake = new HandshakeCapability(serverCommunication, opcua_server, this.getEndpoint_object(),  capabilityId,  capabilityRole);
        this.capabilities.add(handshake); //adding the handshake capability to the list of the capabilities
        wiring = new WiringCapability(serverCommunication, opcua_server, this.getEndpoint_object(),  capabilityId,  capabilityRole );
        this.capabilities.add(wiring); //adding the Wiring capability to the list of the capabilities

    }
}
