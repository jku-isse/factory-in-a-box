package fiab.mes.transport.actor.transportmodule;

import ProcessCore.AbstractCapability;
import fiab.mes.capabilities.ComparableCapability;

public interface WellknownTransportModuleCapability {

	enum SimpleMessageTypes {
		SubscribeState, Reset, Stop
	}

	public static String TRANSPORT_MODULE_UPCUA_TRANSPORT_REQUEST = "TransportRequest";

	public static final String TRANSPORT_MODULE_NORTH_SERVER = "NORTH_SERVER";
	public static final String TRANSPORT_MODULE_SOUTH_SERVER = "SOUTH_SERVER";
	public static final String TRANSPORT_MODULE_EAST_SERVER = "EAST_SERVER";
	public static final String TRANSPORT_MODULE_WEST_SERVER = "WEST_SERVER";
	public static final String TRANSPORT_MODULE_NORTH_CLIENT = "NORTH_CLIENT";
	public static final String TRANSPORT_MODULE_SOUTH_CLIENT = "SOUTH_CLIENT";
	public static final String TRANSPORT_MODULE_EAST_CLIENT = "EAST_CLIENT";
	public static final String TRANSPORT_MODULE_WEST_CLIENT = "WEST_CLIENT";
	public static final String TRANSPORT_MODULE_SELF = "TRANSPORT_MODULE_SELF";

	public static String TURNTABLE_CAPABILITY_URI = "http://factory-in-a-box.fiab/capabilities/turntable";
	
	
	public static AbstractCapability getTurntableCapability() {
		ComparableCapability ac = new ComparableCapability();
		ac.setDisplayName("turntable");
		ac.setID("Capability.Turntable");
		ac.setUri(TURNTABLE_CAPABILITY_URI);
		return ac;
	}
	
}
