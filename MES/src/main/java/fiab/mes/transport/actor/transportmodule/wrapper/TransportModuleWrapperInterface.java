package fiab.mes.transport.actor.transportmodule.wrapper;


import fiab.core.capabilities.transport.TransportModuleRequest;

public interface TransportModuleWrapperInterface {
	
	public void transport(TransportModuleRequest req);
	
	public void stop();
	
	public void reset();
	
	public void subscribeToStatus();
	
	public void unsubscribeFromStatus();

}
