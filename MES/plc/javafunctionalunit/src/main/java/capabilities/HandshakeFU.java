package capabilities;

import communication.Communication;
import communication.utils.Pair;
import helper.CapabilityId;
import helper.CapabilityRole;
import helper.CapabilityType;

public class HandshakeFU extends FunctionUnit implements CapabilityListener {

   // HandshakeCapability handshake;
    WiringCapability wiring;
    Communication communication;
    Object opcua_client;
    Object opcua_server;




    public HandshakeFU(Communication communication, Object opcua_server, Object opcua_client, Object parentObjectId) {
        //calling the Endpoint parent class constructor to init an opcua object to be the parent node for the sub-capabilities
        super(communication.getServerCommunication(), opcua_server, parentObjectId, CapabilityType.HANDSHAKE.toString() + "_FUs", CapabilityType.HANDSHAKE);

        this.communication = communication;
        this.opcua_server = opcua_server;
        this.opcua_client = opcua_client;


        communication.getServerCommunication().addStringMethod(communication.getServerCommunication(), opcua_server, this.getFunctionUnit_object(), new Pair<>(1, (CapabilityType.HANDSHAKE.toString() + "_FU_") + "COMPLETE"), "COMPLETE",
                opcuaMethodInput -> {
                    return compelete();
                });
             communication.getServerCommunication().addStringMethod(communication.getServerCommunication(), opcua_server, this.getFunctionUnit_object(), new Pair<>(1, (CapabilityType.HANDSHAKE.toString() + "_FU_") + "STOP"), "STOP",
                opcuaMethodInput -> {
                    return stop(opcuaMethodInput);
                });
        communication.getServerCommunication().addStringMethod(communication.getServerCommunication(), opcua_server, this.getFunctionUnit_object(), new Pair<>(1, (CapabilityType.HANDSHAKE.toString() + "_FU_") + "RESET"), "RESET",
                opcuaMethodInput -> {
                    return reset(opcuaMethodInput);
                });

        communication.getServerCommunication().addStringMethod(communication.getServerCommunication(), opcua_server, this.getFunctionUnit_object(), new Pair<>(1,("CAPABILITY_"+CapabilityType.HANDSHAKE.toString()+ "_FU_")+"INIT_HANDOVER"), "INIT_HANDOVER",
                opcuaMethodInput -> {

                    return initiateLoading(opcuaMethodInput);
                });
        communication.getServerCommunication().addStringMethod(communication.getServerCommunication(), opcua_server, this.getFunctionUnit_object(), new Pair<>(1,("CAPABILITY_"+CapabilityType.HANDSHAKE.toString()+ "_FU_")+"INIT_UNLOADING"), "INIT_UNLOADING",
                opcuaMethodInput -> {
                    return initiateUnloading(opcuaMethodInput);
                });
        communication.getServerCommunication().addStringMethod(communication.getServerCommunication(), opcua_server, this.getFunctionUnit_object(), new Pair<>(1,("CAPABILITY_"+CapabilityType.HANDSHAKE.toString()+ "_FU_")+"SET_WIRING"), "SET_WIRING",
                opcuaMethodInput -> {
                    return setWiringInfo(); // the opcua method callback is received here

                });
        Object state_nodeid = communication.getServerCommunication().addStringVariableNode(opcua_server, this.getFunctionUnit_object(), new Pair<>(1, (CapabilityType.HANDSHAKE.toString() + "_FU_") + "STATE"), "STATE");
        communication.getServerCommunication().writeVariable(opcua_server, state_nodeid, "IDLE");

        //initializing the main capabilities for the handshake FU
        // handshake = new HandshakeCapability(serverCommunication, opcua_server, this.getEndpoint_object(), capabilityId);
        //   this.capabilities.add(handshake); //adding the handshake capability to the list of the capabilities
        //  handshake.addEventListener(this);

    }

    private String setWiringInfo() {
        return "";
    }

    private String initiateLoading(String opcuaMethodInput) {
        return "";
    }

    private String initiateUnloading(String opcuaMethodInput) {
        return "";
    }

    private String start(String opcuaMethodInput) {
        return "";
    }

    private String ready(String opcuaMethodInput) {
        return "";
    }

    private String reset(String opcuaMethodInput) {
        return "";

    }

    private String stop(String opcuaMethodInput) {
        return "";
    }

    private String compelete() {
        return "";
    }

    public HandshakeFU(Communication communication, Object opcua_server, Object opcua_client, Object parentObjectId, CapabilityId capabilityId) {
        //calling the Endpoint parent class constructor to init an opcua object to be the parent node for the sub-capabilities
        super(communication.getServerCommunication(), opcua_server, parentObjectId, CapabilityType.HANDSHAKE.toString() + "_FU", CapabilityType.HANDSHAKE);
        this.communication = communication;
        this.opcua_client = opcua_client;

        Object state_nodeid = communication.getServerCommunication().addStringVariableNode(opcua_server, this.getFunctionUnit_object(), new Pair<>(1, ("CAPABILITY_" + CapabilityType.HANDSHAKE.toString() + "_") + capabilityId.toString() + "_" + "STATE"), "STATE");
        communication.getServerCommunication().writeVariable(opcua_server, state_nodeid, "CLIENT IDLE");

        // super(communication.getClientCommunication(), opcua_client, parentObjectId, CapabilityType.HANDSHAKE.toString() + "_FU", capabilityId, CapabilityType.HANDSHAKE, CapabilityRole.Required);

        //initializing the main capabilities for the handshake FU
      //  handshake = new HandshakeCapability(communication, opcua_server, opcua_client, this.getFunctionUnit_object(), capabilityId);
       // this.capabilities.add(handshake); //adding the handshake capability to the list of the capabilities



    }

    public HandshakeCapability addHanshakeEndpoint(CapabilityId capabilityId, CapabilityRole capabilityRole) {
        //TODO : double check the CopabilityID is not added before
        Object endpoint_NodeId = communication.getServerCommunication().createNodeString(1, "END_POINT_" + capabilityId.toString() + "_" + capabilityRole.toString());
        communication.getServerCommunication().addNestedObject(opcua_server, getFunctionUnit_object(), endpoint_NodeId, "HANDSHAKE_ENDPOINT");

        Object capabilities_NodeId = communication.getServerCommunication().createNodeString(1, "CAPABILITIES" + capabilityId.toString());
        communication.getServerCommunication().addNestedObject(opcua_server,endpoint_NodeId, capabilities_NodeId, "CAPABILITIES");

        HandshakeCapability handshake;


        if(capabilityRole.equals(CapabilityRole.Required)){

            handshake  = new HandshakeCapability(communication, opcua_server, opcua_client, endpoint_NodeId,capabilities_NodeId, capabilityId);
        }
        else {
            handshake  = new HandshakeCapability(communication.getServerCommunication(), opcua_server,  endpoint_NodeId,capabilities_NodeId, capabilityId);

        }
        handshake.addEventListener(this);

        this.capabilities.add(handshake);
        return handshake;
    }
    public void addWiringCapability(CapabilityId capabilityId,Object endpoint_NodeId,Object capabilities_NodeId) {
        wiring = new WiringCapability(communication.getServerCommunication(), opcua_server, endpoint_NodeId,capabilities_NodeId, capabilityId);
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
    }
    @Override
    public void eventOccurred(CapabilityEvent evt, Capability source) {
        if (evt.getClass().getName().equals("capabilities.stopHandshakeEvent")) {

        } else if (evt.getClass().getName().equals("capabilities.startHandshakeEvent")) {

         //   WiringInformation wiringInfo = wiring.getWiring(handshake.getCurrentCapabilityId());
          //  handshake.starting(wiringInfo, handshake.getCurrentOrderId());

            //  handshake.changeState(HandshakeStates.EXECUTE);
        } else if (evt.getClass().getName().equals("capabilities.initUnloadingHandshakeEvent")) {

        }
    }
}
