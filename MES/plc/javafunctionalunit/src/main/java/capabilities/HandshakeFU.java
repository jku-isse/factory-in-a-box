package capabilities;

import capabilities.Capability;
import communication.Communication;
import communication.open62communication.ClientCommunication;
import communication.open62communication.ServerCommunication;
import communication.utils.RequestedNodePair;
import helper.CapabilityId;
import helper.CapabilityRole;
import helper.CapabilityType;

import java.util.EventObject;
import java.util.List;

public class HandshakeFU extends Endpoint {


    public HandshakeCapability getHandshakeCapability() {
        return handshake;
    }

    public void setHandshakeCapability(HandshakeCapability handshake) {
        this.handshake = handshake;
    }

    HandshakeCapability handshake;
    WiringCapability wiring;

    public HandshakeFU(ServerCommunication serverCommunication, Object opcua_server, Object parentObjectId, CapabilityId capabilityId ) {
        //calling the Endpoint parent class constructor to init an opcua object to be the parent node for the sub-capabilities
        super(serverCommunication, opcua_server, parentObjectId,  CapabilityType.HANDSHAKE.toString()+"_FU", capabilityId, CapabilityType.HANDSHAKE, CapabilityRole.Provided);

        Object state_nodeid = serverCommunication.addStringVariableNode(opcua_server, this.getEndpoint_object(), new RequestedNodePair<>(1, serverCommunication.getUnique_id()), "STATE");
        serverCommunication.writeVariable(opcua_server, state_nodeid, "IDLE");
       //initializing the main capabilities for the handshake FU
        handshake = new HandshakeCapability(serverCommunication, opcua_server, this.getEndpoint_object(),  capabilityId);
        this.capabilities.add(handshake); //adding the handshake capability to the list of the capabilities
        wiring = new WiringCapability(serverCommunication, opcua_server, this.getEndpoint_object(),  capabilityId );
        this.capabilities.add(wiring); //adding the Wiring capability to the list of the capabilities

        wiring.addMyEventListener(new CapabilityListener() {
            public void eventOccurred(CapabilityEvent evt) {
                System.out.println("FROM THE HANDSHAKE FUUUUUUUUUUU THE WIRING IS DONE");
            }
        });

    }

    public HandshakeFU(ClientCommunication clientCommunication, Object opcua_client, Object parentObjectId, CapabilityId capabilityId ) {
        //calling the Endpoint parent class constructor to init an opcua object to be the parent node for the sub-capabilities
        super(clientCommunication, opcua_client, parentObjectId,  CapabilityType.HANDSHAKE.toString()+"_FU", capabilityId, CapabilityType.HANDSHAKE, CapabilityRole.Required);

         //initializing the main capabilities for the handshake FU
      //  handshake = new HandshakeCapability(clientCommunication, opcua_client, this.getEndpoint_object(),  capabilityId);
        this.capabilities.add(handshake); //adding the handshake capability to the list of the capabilities


    }
}
