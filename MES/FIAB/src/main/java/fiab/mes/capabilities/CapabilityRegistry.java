package fiab.mes.capabilities;

import ProcessCore.AbstractCapability;
import fiab.mes.general.ComparableCapability;

public interface CapabilityRegistry {
/** 
 * Method for registering key/value pairs to the registry.
 * 
 * @param 	cap 	the capability which should be added to the registry
 * @return			returns true by successful adding of the element, false otherwise 	
 */
	public boolean register(AbstractCapability cap);
/**
 * Gets the value for a give key.
 * 
 * @param id	the key value should be found for
 * @return		returns the capability, null if not found
 */
	public ComparableCapability get(String id);
}
