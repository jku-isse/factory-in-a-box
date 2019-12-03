package fiab.mes.transport.handshake;

import ProcessCore.AbstractCapability;
import fiab.mes.general.ComparableCapability;

public class HandshakeProtocol {

	public enum ServerSide {
		Stopping, Stopped, Resetting, IdleLoaded, IdleEmpty, Starting, Preparing, ReadyLoaded, ReadyEmpty, Execute, Completing, Completed
	}
	
	public static final String STATE_SERVERSIDE_VAR_NAME = "HANDSHAKE_SERVERSIDE_STATE";
	
	public enum ClientSide {
		Stopping, Stopped, Resetting, Idle, Starting, Initiating, Initiated, Ready, Execute, Completing, Completed
	}
	
	public static final String STATE_CLIENTSIDE_VAR_NAME = "HANDSHAKE_CLIENTSIDE_STATE";
	
	
	public static AbstractCapability getHandshakeCapability() {
		ComparableCapability ac = new ComparableCapability();
		ac.setDisplayName("Execute Pallet Handover Handshake");
		ac.setID("Capability.Handshake");
		ac.setUri("http://factory-in-a-box.fiab/capabilities/handshake");
		return ac;
	}
	
	public static AbstractCapability getInputStationCapability() {
		ComparableCapability ac = new ComparableCapability();
		ac.setDisplayName("InputStation");
		ac.setID("Capability.InputStation");
		ac.setUri("http://factory-in-a-box.fiab/capabilities/inputstation");
		ac.getCapabilities().add(getHandshakeCapability());
		return ac;
	}
	
	public static AbstractCapability getOutputStationCapability() {
		ComparableCapability ac = new ComparableCapability();
		ac.setDisplayName("OutputStation");
		ac.setID("Capability.OutputStation");
		ac.setUri("http://factory-in-a-box.fiab/capabilities/outputstation");
		ac.getCapabilities().add(getHandshakeCapability());
		return ac;
	}
	
	
}
