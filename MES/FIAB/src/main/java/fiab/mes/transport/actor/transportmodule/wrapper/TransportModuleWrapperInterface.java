package fiab.mes.transport.actor.transportmodule.wrapper;

import fiab.turntable.actor.InternalTransportModuleRequest;

public interface TransportModuleWrapperInterface {
	
	public void transport(InternalTransportModuleRequest req);
	
	public void stop();
	
	public void reset();
	
	public void subscribeToStatus();
	
	public void unsubscribeFromStatus();

}
