package fiab.mes.transport.actor.transportmodule;

import ProcessCore.AbstractCapability;
import fiab.mes.general.ComparableCapability;

public interface WellknownTransportModuleCapability {

	public static final String TRANSPORT_MODULE_NORTH_SERVER = "NORTH_SERVER";
	public static final String TRANSPORT_MODULE_SOUTH_SERVER = "SOUTH_SERVER";
	public static final String TRANSPORT_MODULE_EAST_SERVER = "EAST_SERVER";
	public static final String TRANSPORT_MODULE_WEST_SERVER = "WEST_SERVER";
	public static final String TRANSPORT_MODULE_NORTH_CLIENT = "NORTH_CLIENT";
	public static final String TRANSPORT_MODULE_SOUTH_CLIENT = "SOUTH_CLIENT";
	public static final String TRANSPORT_MODULE_EAST_CLIENT = "EAST_CLIENT";
	public static final String TRANSPORT_MODULE_WEST_CLIENT = "WEST_CLIENT";
	public static final String TRANSPORT_MODULE_SELF = "TRANSPORT_MODULE_SELF";

	public static AbstractCapability getTurntableCapability() {
		ComparableCapability ac = new ComparableCapability();
		ac.setDisplayName("turntable");
		ac.setID("Capability.Turntable");
		ac.setID("http://factory-in-a-box.fiab/capabilities/turntable");
		return ac;
	}
	
}
