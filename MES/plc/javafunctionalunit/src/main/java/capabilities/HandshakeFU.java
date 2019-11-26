package capabilities;

import communication.Communication;
import communication.open62communication.ServerCommunication;
import communication.utils.RequestedNodePair;
import helper.CapabilityId;
import helper.CapabilityRole;
import helper.CapabilityType;

public class HandshakeFU extends Endpoint implements CapabilityListener {

    HandshakeCapability handshake;
    WiringCapability wiring;
    Communication communication;
    Object opcua_client;

    public HandshakeFU(ServerCommunication serverCommunication, Object opcua_server, Object parentObjectId, CapabilityId capabilityId) {
        //calling the Endpoint parent class constructor to init an opcua object to be the parent node for the sub-capabilities
        super(serverCommunication, opcua_server, parentObjectId, CapabilityType.HANDSHAKE.toString() + "_FU", CapabilityType.HANDSHAKE, CapabilityRole.Provided);

        Object state_nodeid = serverCommunication.addStringVariableNode(opcua_server, this.getEndpoint_object(), new RequestedNodePair<>(1, serverCommunication.getUnique_id()), "STATE");
        serverCommunication.writeVariable(opcua_server, state_nodeid, "IDLE");

        //initializing the main capabilities for the handshake FU
        handshake = new HandshakeCapability(serverCommunication, opcua_server, this.getEndpoint_object(), capabilityId);
        this.capabilities.add(handshake); //adding the handshake capability to the list of the capabilities
        handshake.addEventListener(this);

    }

    public HandshakeFU(Communication communication, Object opcua_server, Object opcua_client, Object parentObjectId, CapabilityId capabilityId) {
        //calling the Endpoint parent class constructor to init an opcua object to be the parent node for the sub-capabilities
        super(communication.getServerCommunication(), opcua_server, parentObjectId, CapabilityType.HANDSHAKE.toString() + "_FU", CapabilityType.HANDSHAKE, CapabilityRole.Provided);
        this.communication = communication;
        this.opcua_client = opcua_client;

        Object state_nodeid = communication.getServerCommunication().addStringVariableNode(opcua_server, this.getEndpoint_object(), new RequestedNodePair<>(1, communication.getServerCommunication().getUnique_id()), "STATE");
        communication.getServerCommunication().writeVariable(opcua_server, state_nodeid, "CLIENT IDLE");

        // super(communication.getClientCommunication(), opcua_client, parentObjectId, CapabilityType.HANDSHAKE.toString() + "_FU", capabilityId, CapabilityType.HANDSHAKE, CapabilityRole.Required);

        //initializing the main capabilities for the handshake FU
        handshake = new HandshakeCapability(communication, opcua_server, opcua_client, this.getEndpoint_object(), capabilityId);
        this.capabilities.add(handshake); //adding the handshake capability to the list of the capabilities
        wiring = new WiringCapability(communication.getServerCommunication(), opcua_server, this.getEndpoint_object(), capabilityId);
        this.capabilities.add(wiring); //adding the Wiring capability to the list of the capabilities

        wiring.addEventListener(new CapabilityListener() {
            public void eventOccurred(CapabilityEvent evt, Capability source) {
                if (evt.getClass().getName().equals("capabilities.WiringCapabilityEvent")) {
                  //  String serverUrl =  ((WiringCapability) source).getWiring(handshake.getCurrentCapabilityId());
                   // handshake.setCapabilityId();
                    System.out.println("FROM THE HANDSHAKE FU THE WIRING IS DONE " + ((WiringCapability) source).wiringMap.size());
                }
            //    System.out.println("FROM THE HANDSHAKE FUUUUUUUUUUU THE WIRING IS DONE " + ((WiringCapability) source).wiringMap.size());
            }
        });

        handshake.addEventListener(this);
    }


    @Override
    public void eventOccurred(CapabilityEvent evt, Capability source) {
        if (evt.getClass().getName().equals("capabilities.stopHandshakeEvent")) {

        } else if (evt.getClass().getName().equals("capabilities.startHandshakeEvent")) {

            WiringInformation wiringInfo = wiring.getWiring(handshake.getCurrentCapabilityId());
           handshake.starting(wiringInfo, handshake.getCurrentOrderId());

          //  handshake.changeState(HandshakeStates.EXECUTE);
        } else if (evt.getClass().getName().equals("capabilities.initUnloadingHandshakeEvent")) {

        }
    }
}
