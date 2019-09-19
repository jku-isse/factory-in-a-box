package capabilities;

import capabilities.Capability;
import communication.Communication;
import communication.open62communication.ServerCommunication;
import helper.CapabilityId;
import helper.CapabilityRole;
import helper.CapabilityType;

import java.util.List;

public class HandshakeFU extends Endpoint {


    public HandshakeFU(ServerCommunication serverCommunication, Object opcua_server, Object parentObjectId, CapabilityId capabilityId , CapabilityRole capabilityRole) {
        super(serverCommunication, opcua_server, parentObjectId,  CapabilityType.HANDSHAKE_FU.toString(), capabilityId, CapabilityType.HANDSHAKE_FU, capabilityRole);

      Capability wiring = new Capability(serverCommunication, opcua_server, this.getEndpoint_object(),  capabilityId, CapabilityType.WIRING, capabilityRole);
        //   this.capabilities = capabilities;
    }
}
