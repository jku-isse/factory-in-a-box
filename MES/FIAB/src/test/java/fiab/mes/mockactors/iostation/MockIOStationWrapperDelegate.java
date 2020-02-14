package fiab.mes.mockactors.iostation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import fiab.mes.machine.actor.iostation.wrapper.IOStationWrapperInterface;
import fiab.mes.transport.handshake.HandshakeProtocol;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerMessageTypes;

public class MockIOStationWrapperDelegate implements IOStationWrapperInterface {

	protected ActorRef wrapper;
	private static final Logger logger = LoggerFactory.getLogger(MockIOStationWrapperDelegate.class);
	
	public MockIOStationWrapperDelegate(ActorRef wrapper) {
		this.wrapper = wrapper;
	}
	
	@Override
	public void stop() {
		logger.info("stop called");
		wrapper.tell(HandshakeProtocol.ServerMessageTypes.Stop, ActorRef.noSender());
	}

	@Override
	public void reset() {
		logger.info("reset called");
		wrapper.tell(HandshakeProtocol.ServerMessageTypes.Reset, ActorRef.noSender());
	}

	@Override
	public void subscribeToStatus() {
		logger.info("subscribeToStatus called");
		wrapper.tell(HandshakeProtocol.ServerMessageTypes.SubscribeToStateUpdates, ActorRef.noSender());
	}

	@Override
	public void subscribeToLoadStatus() {
		logger.error("NOT IMPLEMENTED subscribeToLoadStatus called");
	//	throw new RuntimeException("Not implemented");
	}

}
