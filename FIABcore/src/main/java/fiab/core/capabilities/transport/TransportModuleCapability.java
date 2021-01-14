package main.java.fiab.core.capabilities.transport;

import ProcessCore.AbstractCapability;
import main.java.fiab.core.capabilities.ComparableCapability;
import main.java.fiab.core.capabilities.OPCUABasicMachineBrowsenames;

public interface TransportModuleCapability extends OPCUABasicMachineBrowsenames {

	public static String OPCUA_TRANSPORT_REQUEST = "TransportRequest";

	public static String TRANSPORT_CAPABILITY_URI = "http://factory-in-a-box.fiab/capabilities/transport";
	
	public static AbstractCapability getTransportCapability() {
		ComparableCapability ac = new ComparableCapability();
		ac.setDisplayName("transport");
		ac.setID("Capability.Transport");
		ac.setUri(TRANSPORT_CAPABILITY_URI);
		return ac;
	}
}
