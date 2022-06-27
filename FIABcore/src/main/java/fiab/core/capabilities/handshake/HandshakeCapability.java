package fiab.core.capabilities.handshake;

import ProcessCore.AbstractCapability;
import fiab.core.capabilities.ComparableCapability;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;

public interface HandshakeCapability {

	String SERVER_CAPABILITY_ID = "HANDSHAKE_FU";
	
	enum ServerMessageTypes {
		Reset, Stop, RequestInitiateHandover, OkResponseInitHandover, NotOkResponseInitHandover, 
		RequestStartHandover, OkResponseStartHandover, NotOkResponseStartHandover, Complete, 
		SubscribeToStateUpdates, UnsubscribeToStateUpdates 
	}
	
	String OPCUA_STATE_SERVERSIDE_VAR_NAME = "STATE"; //"HANDSHAKE_SERVERSIDE_STATE";
	
	String OPCUA_EXTERNAL_SERVERSIDE_INIT_HANDOVER_REQUEST = "INIT_HANDOVER";
	String OPCUA_EXTERNAL_SERVERSIDE_START_HANDOVER_REQUEST = "START_HANDOVER";
	String OPCUA_INTERNAL_SERVERSIDE_COMPLETE_REQUEST = "COMPLETE";
	String OPCUA_INTERNAL_SERVERSIDE_STOP_REQUEST = OPCUABasicMachineBrowsenames.STOP_REQUEST;
	String OPCUA_INTERNAL_SERVERSIDE_RESET_REQUEST = OPCUABasicMachineBrowsenames.RESET_REQUEST;

	String CLIENT_CAPABILITY_ID = "HANDSHAKE_FU";
	
	enum ClientMessageTypes {
		Reset, Stop, Start, Complete
	}

	String OPCUA_INTERNAL_CLIENTSIDE_COMPLETE_REQUEST = "COMPLETE";
	String OPCUA_INTERNAL_CLIENTSIDE_START_REQUEST = "START";
	String OPCUA_INTERNAL_CLIENTSIDE_STOP_REQUEST = OPCUABasicMachineBrowsenames.STOP_REQUEST;
	String OPCUA_INTERNAL_CLIENTSIDE_RESET_REQUEST = OPCUABasicMachineBrowsenames.RESET_REQUEST;

	
	enum StateOverrideRequests {
		SetLoaded, SetEmpty
	}
	String OPCUA_INTERNAL_LOADEDSTATEOVERRIDE_REQUEST = "SET_LOADED";
	String OPCUA_INTERNAL_EMPTYSTATEOVERRIDE_REQUEST = "SET_EMPTY";
	
	String OPCUA_STATE_CLIENTSIDE_VAR_NAME = "STATE"; //"HANDSHAKE_CLIENTSIDE_STATE";

	String HANDSHAKE_CAPABILITY_URI = "http://factory-in-a-box.fiab/capabilities/handshake";
	
	static AbstractCapability getHandshakeCapability() {
		ComparableCapability ac = new ComparableCapability();
		ac.setDisplayName("Execute Pallet Handover Handshake");
		ac.setID("Capability.Handshake");
		ac.setUri(HANDSHAKE_CAPABILITY_URI);
		return ac;
	}
}
