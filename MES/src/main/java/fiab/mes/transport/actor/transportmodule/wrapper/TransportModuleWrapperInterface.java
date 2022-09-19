package fiab.mes.transport.actor.transportmodule.wrapper;


import fiab.core.capabilities.transport.TransportRequest;

public interface TransportModuleWrapperInterface {
	
	public void transport(TransportRequest req);
	
	public void stop();
	
	public void reset();
	
	public void subscribeToStatus();
	
	public void unsubscribeFromStatus();

}
