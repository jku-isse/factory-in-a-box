package fiab.core.capabilities.transport;

public interface TurntableModuleWellknownCapabilityIdentifiers extends TransportModuleCapability {

	enum SimpleMessageTypes {
		SubscribeState, Reset, Stop
	}

	String TRANSPORT_MODULE_NORTH_SERVER = "NORTH_SERVER";
	String TRANSPORT_MODULE_SOUTH_SERVER = "SOUTH_SERVER";
	String TRANSPORT_MODULE_EAST_SERVER = "EAST_SERVER";
	String TRANSPORT_MODULE_WEST_SERVER = "WEST_SERVER";
	String TRANSPORT_MODULE_NORTH_CLIENT = "NORTH_CLIENT";
	String TRANSPORT_MODULE_SOUTH_CLIENT = "SOUTH_CLIENT";
	String TRANSPORT_MODULE_EAST_CLIENT = "EAST_CLIENT";
	String TRANSPORT_MODULE_WEST_CLIENT = "WEST_CLIENT";
	String TRANSPORT_MODULE_SELF = "TRANSPORT_MODULE_SELF";


	
}
