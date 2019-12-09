package fiab.mes.mockactors.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import fiab.mes.mockactors.transport.MockTransportModuleWrapper.SimpleMessageTypes;
import fiab.mes.transport.actor.transportmodule.wrapper.TransportModuleWrapperInterface;
import fiab.mes.transport.msg.TransportModuleRequest;

public class MockTransportModuleWrapperDelegate implements TransportModuleWrapperInterface {

	private static final Logger logger = LoggerFactory.getLogger(MockTransportModuleWrapperDelegate.class);
	
	private ActorRef wrapper;
	
	MockTransportModuleWrapperDelegate(ActorRef wrapper) {
		this.wrapper = wrapper;
	}
	
	@Override
	public void transport(TransportModuleRequest req) {
		logger.info("transport called");
		wrapper.tell(req, ActorRef.noSender());
	}

	@Override
	public void stop() {
		logger.info("stop called");
		wrapper.tell(SimpleMessageTypes.Stop, ActorRef.noSender());
	}

	@Override
	public void reset() {
		logger.info("reset called");
		wrapper.tell(SimpleMessageTypes.Reset, ActorRef.noSender());
	}

	@Override
	public void subscribeToStatus() {
		logger.info("subscribeToStatus called");
		wrapper.tell(SimpleMessageTypes.SubscribeState, ActorRef.noSender());
	}

}
