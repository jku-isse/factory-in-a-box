package fiab.mes.transport.handshake;

import ProcessCore.AbstractCapability;
import fiab.mes.capabilities.ComparableCapability;

public class HandshakeProtocol {

	public enum ServerSide {
		UNKNOWN,STOPPING, STOPPED, RESETTING, IDLE_LOADED, IDLE_EMPTY, STARTING, PREPARING, READY_LOADED, READY_EMPTY, EXECUTE, COMPLETING, COMPLETE
	}
	
	public static final String STATE_SERVERSIDE_VAR_NAME = "STATE"; //"HANDSHAKE_SERVERSIDE_STATE";
	
	public enum ClientSide {
		STOPPING, STOPPED, RESETTING, IDLE, STARTING, INITIATING, INITIATED, READY, EXECUTE, COMPLETING, COMPLETED
	}
	
	public static enum ServerMessageTypes {
		Reset, Stop, RequestInitiateHandover, OkResponseInitHandover, NotOkResponseInitHandover, 
		RequestStartHandover, OkResponseStartHandover, NotOkResponseStartHandover, Complete, 
		SubscribeToStateUpdates, UnsubscribeToStateUpdates 
	}

	public static enum ClientMessageTypes {
		Reset, Stop, Start, Complete
	}

	public static final String STATE_CLIENTSIDE_VAR_NAME = "STATE"; //"HANDSHAKE_CLIENTSIDE_STATE";
	
	public static final String IOSTATION_PROVIDED_OPCUA_METHOD_RESET = "RESET";
	public static final String IOSTATION_PROVIDED_OPCUA_METHOD_STOP = "STOP";
	public static final String IOSTATION_PROVIDED_OPCUA_STATE_VAR = "STATE";
	
	public static final String INPUTSTATION_CAPABILITY_URI = "http://factory-in-a-box.fiab/capabilities/inputstation";
	public static final String OUTPUTSTATION_CAPABILITY_URI = "http://factory-in-a-box.fiab/capabilities/outputstation";
	public static final String HANDSHAKE_CAPABILITY_URI = "http://factory-in-a-box.fiab/capabilities/handshake";
	
	public static AbstractCapability getHandshakeCapability() {
		ComparableCapability ac = new ComparableCapability();
		ac.setDisplayName("Execute Pallet Handover Handshake");
		ac.setID("Capability.Handshake");
		ac.setUri(HANDSHAKE_CAPABILITY_URI);
		return ac;
	}
	
	public static AbstractCapability getInputStationCapability() {
		ComparableCapability ac = new ComparableCapability();
		ac.setDisplayName("InputStation");
		ac.setID("Capability.InputStation");
		ac.setUri(INPUTSTATION_CAPABILITY_URI);
		ac.getCapabilities().add(getHandshakeCapability());
		return ac;
	}
	
	public static AbstractCapability getOutputStationCapability() {
		ComparableCapability ac = new ComparableCapability();
		ac.setDisplayName("OutputStation");
		ac.setID("Capability.OutputStation");
		ac.setUri(OUTPUTSTATION_CAPABILITY_URI);
		ac.getCapabilities().add(getHandshakeCapability());
		return ac;
	}
	
	
}
