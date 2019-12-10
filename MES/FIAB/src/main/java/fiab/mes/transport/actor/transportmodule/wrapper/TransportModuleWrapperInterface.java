package fiab.mes.transport.actor.transportmodule.wrapper;

import fiab.mes.transport.msg.TransportModuleRequest;

public interface TransportModuleWrapperInterface {
	
	public void transport(TransportModuleRequest req);
	
	public void stop();
	
	public void reset();
	
	public void subscribeToStatus();

}
