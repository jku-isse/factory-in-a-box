package fiab.core.capabilities.handshake;

import ProcessCore.AbstractCapability;
import fiab.core.capabilities.ComparableCapability;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;

public interface IOStationCapability extends HandshakeCapability, OPCUABasicMachineBrowsenames{

// Reuse those of OPCUABasicMachineBrowsenames		
//	public static final String IOSTATION_PROVIDED_OPCUA_METHOD_RESET = "RESET";
//	public static final String IOSTATION_PROVIDED_OPCUA_METHOD_STOP = "STOP";
//	public static final String IOSTATION_PROVIDED_OPCUA_STATE_VAR = "STATE";
	
	String INPUTSTATION_CAPABILITY_URI = "http://factory-in-a-box.fiab/capabilities/inputstation";
	String OUTPUTSTATION_CAPABILITY_URI = "http://factory-in-a-box.fiab/capabilities/outputstation";

	String INPUTSTATION_CAPABILITY_ID = "InputStation";
	String OUTPUTSTATION_CAPABILITY_ID = "OutputStation";
		
	static AbstractCapability getInputStationCapability() {
		ComparableCapability ac = new ComparableCapability();
		ac.setDisplayName(INPUTSTATION_CAPABILITY_ID);
		ac.setID("Capability.InputStation");
		ac.setUri(INPUTSTATION_CAPABILITY_URI);
		ac.getCapabilities().add(HandshakeCapability.getHandshakeCapability());
		return ac;
	}
	
	static AbstractCapability getOutputStationCapability() {
		ComparableCapability ac = new ComparableCapability();
		ac.setDisplayName(OUTPUTSTATION_CAPABILITY_ID);
		ac.setID("Capability.OutputStation");
		ac.setUri(OUTPUTSTATION_CAPABILITY_URI);
		ac.getCapabilities().add(HandshakeCapability.getHandshakeCapability());
		return ac;
	}
}
