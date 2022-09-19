package fiab.core.capabilities.transport;

import ProcessCore.AbstractCapability;
import fiab.core.capabilities.ComparableCapability;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.functionalunit.BasicFUBehaviour;

public interface TransportModuleCapability extends OPCUABasicMachineBrowsenames, BasicFUBehaviour {

	String OPCUA_TRANSPORT_REQUEST = "TransportRequest";

	String TRANSPORT_CAPABILITY_URI = "http://factory-in-a-box.fiab/capabilities/transport";

	static AbstractCapability getTransportCapability() {
		ComparableCapability ac = new ComparableCapability();
		ac.setDisplayName("transport");
		ac.setID("Capability.Transport");
		ac.setUri(TRANSPORT_CAPABILITY_URI);
		return ac;
	}

	void transport(TransportRequest req);
}
