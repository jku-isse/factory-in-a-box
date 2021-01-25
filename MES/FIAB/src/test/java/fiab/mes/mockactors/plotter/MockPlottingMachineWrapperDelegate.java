package fiab.mes.mockactors.plotter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import fiab.core.capabilities.plotting.PlotterMessageTypes;
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
		wrapper.tell(PlotterMessageTypes.SubscribeState, ActorRef.noSender());
	}

	@Override
	public void plot(String imageId, String orderId) {
		logger.info("plot called");
		wrapper.tell(PlotterMessageTypes.Plot, ActorRef.noSender());
	}

	@Override
	public void stop() {
		logger.info("stop called");
		wrapper.tell(PlotterMessageTypes.Stop, ActorRef.noSender());
	}

	@Override
	public void reset() {
		logger.info("reset called");
		wrapper.tell(PlotterMessageTypes.Reset, ActorRef.noSender());
	}

}
