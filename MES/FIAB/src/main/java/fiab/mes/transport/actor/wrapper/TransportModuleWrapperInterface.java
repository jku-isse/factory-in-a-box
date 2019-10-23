package fiab.mes.transport.actor.wrapper;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fiab.mes.opcua.Subscription;
import fiab.mes.transport.MachineLevelEventBus;
import fiab.mes.transport.mockClasses.Direction;

public interface TransportModuleWrapperInterface {
	
	public void transport(Direction from, Direction to, String orderId) throws InterruptedException;
	
	public void stopp();
	
	public void reset();

}
