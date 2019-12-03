package fiab.mes.mockactors.iostation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import fiab.mes.machine.actor.iostation.wrapper.IOStationWrapperInterface;
import fiab.mes.mockactors.MockServerHandshakeActor.MessageTypes;

public class MockIOStationWrapperDelegate implements IOStationWrapperInterface {

	protected ActorRef wrapper;
	private static final Logger logger = LoggerFactory.getLogger(MockIOStationWrapperDelegate.class);
	
	@Override
	public void stop() {
		logger.info("stop called");
		wrapper.tell(MessageTypes.Stop, ActorRef.noSender());
	}

	@Override
	public void reset() {
		logger.info("reset called");
		wrapper.tell(MessageTypes.Reset, ActorRef.noSender());
	}

	@Override
	public void subscribeToStatus() {
		logger.info("subscribeToStatus called");
		wrapper.tell(MessageTypes.SubscribeToStateUpdates, ActorRef.noSender());
	}

	@Override
	public void subscribeToLoadStatus() {
		logger.error("NOT IMPLEMENTED subscribeToLoadStatus called");
		throw new RuntimeException("Not implemented");
	}

}
