package fiab.mes.mockactors.iostation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import main.java.fiab.core.capabilities.handshake.IOStationCapability;
import fiab.mes.machine.actor.iostation.wrapper.IOStationWrapperInterface;

public class MockIOStationWrapperDelegate implements IOStationWrapperInterface {

	protected ActorRef wrapper;
	private static final Logger logger = LoggerFactory.getLogger(MockIOStationWrapperDelegate.class);
	
	public MockIOStationWrapperDelegate(ActorRef wrapper) {
		this.wrapper = wrapper;
	}
	
	@Override
	public void stop() {
		logger.info("stop called");
		wrapper.tell(IOStationCapability.ServerMessageTypes.Stop, ActorRef.noSender());
	}

	@Override
	public void reset() {
		logger.info("reset called");
		wrapper.tell(IOStationCapability.ServerMessageTypes.Reset, ActorRef.noSender());
	}

	@Override
	public void subscribeToStatus() {
		logger.info("subscribeToStatus called");
		wrapper.tell(IOStationCapability.ServerMessageTypes.SubscribeToStateUpdates, ActorRef.noSender());
	}

	@Override
	public void subscribeToLoadStatus() {
		logger.error("NOT IMPLEMENTED subscribeToLoadStatus called");
	//	throw new RuntimeException("Not implemented");
	}

}
