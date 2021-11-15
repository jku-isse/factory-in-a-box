package fiab.core.capabilities.handshake;

import ProcessCore.AbstractCapability;
import fiab.core.capabilities.ComparableCapability;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;

public interface HandshakeCapability {

	public enum ServerSideStates {
		UNKNOWN,STOPPING, STOPPED, RESETTING, IDLE_LOADED, IDLE_EMPTY, STARTING, PREPARING, READY_LOADED, READY_EMPTY, EXECUTE, COMPLETING, COMPLETE
	}
	
	public static enum ServerMessageTypes {
		Reset, Stop, RequestInitiateHandover, OkResponseInitHandover, NotOkResponseInitHandover, 
		RequestStartHandover, OkResponseStartHandover, NotOkResponseStartHandover, Complete, 
		SubscribeToStateUpdates, UnsubscribeToStateUpdates 
	}
	
	public static final String OPCUA_STATE_SERVERSIDE_VAR_NAME = "STATE"; //"HANDSHAKE_SERVERSIDE_STATE";
	
	public static final String OPCUA_EXTERNAL_SERVERSIDE_INIT_HANDOVER_REQUEST = "INIT_HANDOVER";
	public static final String OPCUA_EXTERNAL_SERVERSIDE_START_HANDOVER_REQUEST = "START_HANDOVER";
	public static final String OPCUA_INTERNAL_SERVERSIDE_START_REQUEST = "START";
	public static final String OPCUA_INTERNAL_SERVERSIDE_COMPLETE_REQUEST = "COMPLETE";
	public static final String OPCUA_INTERNAL_SERVERSIDE_STOP_REQUEST = OPCUABasicMachineBrowsenames.STOP_REQUEST;
	public static final String OPCUA_INTERNAL_SERVERSIDE_RESET_REQUEST = OPCUABasicMachineBrowsenames.RESET_REQUEST;
	
	
	public enum ClientSideStates {
		STOPPING, STOPPED, RESETTING, IDLE, STARTING, INITIATING, INITIATED, READY, EXECUTE, COMPLETING, COMPLETED
	}
	
	public static enum ClientMessageTypes {
		Reset, Stop, Start, Complete
	}

	public static final String OPCUA_INTERNAL_CLIENTSIDE_COMPLETE_REQUEST = "COMPLETE";
	public static final String OPCUA_INTERNAL_CLIENTSIDE_START_REQUEST = "START";
	public static final String OPCUA_INTERNAL_CLIENTSIDE_STOP_REQUEST = OPCUABasicMachineBrowsenames.STOP_REQUEST;
	public static final String OPCUA_INTERNAL_CLIENTSIDE_RESET_REQUEST = OPCUABasicMachineBrowsenames.RESET_REQUEST;

	
	public static enum StateOverrideRequests {
		SetLoaded, SetEmpty
	}
	public static final String OPCUA_INTERNAL_LOADEDSTATEOVERRIDE_REQUEST = "SET_LOADED";
	public static final String OPCUA_INTERNAL_EMPTYSTATEOVERRIDE_REQUEST = "SET_EMPTY";
	
	
	
	public static final String OPCUA_STATE_CLIENTSIDE_VAR_NAME = "STATE"; //"HANDSHAKE_CLIENTSIDE_STATE";
	
	
	public static final String HANDSHAKE_CAPABILITY_URI = "http://factory-in-a-box.fiab/capabilities/handshake";
	
	public static AbstractCapability getHandshakeCapability() {
		ComparableCapability ac = new ComparableCapability();
		ac.setDisplayName("Execute Pallet Handover Handshake");
		ac.setID("Capability.Handshake");
		ac.setUri(HANDSHAKE_CAPABILITY_URI);
		return ac;
	}
}
