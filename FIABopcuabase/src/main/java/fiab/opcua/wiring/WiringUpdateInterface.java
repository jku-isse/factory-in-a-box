package fiab.opcua.wiring;

import main.java.fiab.core.capabilities.wiring.WiringInfo;

public interface WiringUpdateInterface  {
	public void provideWiringInfo(WiringInfo wiringInfo) throws WiringException;
	
}
