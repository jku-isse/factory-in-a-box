package fiab.mes.mockactors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import fiab.mes.machine.actor.plotter.wrapper.PlottingMachineWrapperInterface;

public class MockPlottingMachineWrapperDelegate implements PlottingMachineWrapperInterface {

	protected ActorRef wrapper;
	private static final Logger logger = LoggerFactory.getLogger(MockPlottingMachineWrapperDelegate.class);
	
	public MockPlottingMachineWrapperDelegate(ActorRef wrapper) {
		this.wrapper = wrapper;
	}
	
	// for now, this is a well behaving wrapper in that we don't mock failing opcua connections etc.
	
	@Override
	public void subscribeToStatus() {
		logger.info("subscribeToStatus called");
		wrapper.tell(MockMachineWrapper.MessageTypes.SubscribeState, ActorRef.noSender());
	}

	@Override
	public void plot(String imageId, String orderId) {
		logger.info("plot called");
		wrapper.tell(MockMachineWrapper.MessageTypes.Plot, ActorRef.noSender());
	}

	@Override
	public void stop() {
		logger.info("stop called");
		wrapper.tell(MockMachineWrapper.MessageTypes.Stop, ActorRef.noSender());
	}

	@Override
	public void reset() {
		logger.info("reset called");
		wrapper.tell(MockMachineWrapper.MessageTypes.Reset, ActorRef.noSender());
	}

}
