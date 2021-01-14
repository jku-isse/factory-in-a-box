package main.java.fiab.core.capabilities.meta;

public interface OPCUACapabilitiesAndWiringInfoBrowsenames {
	public static final String CAPABILITIES = "CAPABILITIES", 
			CAPABILITY = "CAPABILITY", 
			ID = "ID", 
			TYPE = "TYPE",
			ROLE = "ROLE", 
			ROLE_VALUE_REQUIRED = "Required", 
			ROLE_VALUE_PROVIDED = "Provided", 
			WIRING_INFO = "WIRING_INFO",
			LOCAL_CAPABILITYID = "LOCAL_CAPABILITYID", 
			REMOTE_CAPABILITYID ="REMOTE_CAPABILITYID",
			REMOTE_ENDPOINT = "REMOTE_ENDPOINT",
			REMOTE_NODEID = "REMOTE_NODEID", 
			REMOTE_ROLE = "REMOTE_ROLE";
	
	public static final boolean IS_PROVIDED = true;
	public static final boolean IS_REQUIRED = false;
	
	public static String OPCUA_WIRING_REQUEST = "SET_WIRING";
}
