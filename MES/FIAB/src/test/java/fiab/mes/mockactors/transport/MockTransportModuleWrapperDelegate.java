package fiab.mes.mockactors.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import fiab.mes.transport.actor.transportmodule.WellknownTransportModuleCapability;
import fiab.mes.transport.actor.transportmodule.WellknownTransportModuleCapability.SimpleMessageTypes;
import fiab.mes.transport.actor.transportmodule.wrapper.TransportModuleWrapperInterface;
import fiab.mes.transport.msg.InternalTransportModuleRequest;

public class MockTransportModuleWrapperDelegate implements TransportModuleWrapperInterface {

	private static final Logger logger = LoggerFactory.getLogger(MockTransportModuleWrapperDelegate.class);
	
	private ActorRef wrapper;
	
	public MockTransportModuleWrapperDelegate(ActorRef wrapper) {
		this.wrapper = wrapper;
	}
	
	@Override
	public void transport(InternalTransportModuleRequest req) {
		logger.info("transport called");
		wrapper.tell(req, ActorRef.noSender());
	}

	@Override
	public void stop() {
		logger.info("stop called");
		wrapper.tell(WellknownTransportModuleCapability.SimpleMessageTypes.Stop, ActorRef.noSender());
	}

	@Override
	public void reset() {
		logger.info("reset called");
		wrapper.tell(WellknownTransportModuleCapability.SimpleMessageTypes.Reset, ActorRef.noSender());
	}

	@Override
	public void subscribeToStatus() {
		logger.info("subscribeToStatus called");
		wrapper.tell(WellknownTransportModuleCapability.SimpleMessageTypes.SubscribeState, ActorRef.noSender());
	}

	@Override
	public void unsubscribeFromStatus() {
		// TODO Auto-generated method stub
		
	}

}
