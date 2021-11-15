package fiab.mes.mockactors.foldingstation;

import akka.actor.ActorRef;
import fiab.core.capabilities.folding.FoldingMessageTypes;
import fiab.core.capabilities.plotting.PlotterMessageTypes;
import fiab.mes.machine.actor.foldingstation.wrapper.FoldingStationWrapperInterface;
import fiab.mes.machine.actor.plotter.wrapper.PlottingMachineWrapperInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockFoldingStationWrapperDelegate implements FoldingStationWrapperInterface {

	protected ActorRef wrapper;
	private static final Logger logger = LoggerFactory.getLogger(MockFoldingStationWrapperDelegate.class);

	public MockFoldingStationWrapperDelegate(ActorRef wrapper) {
		this.wrapper = wrapper;
	}

	// for now, this is a well behaving wrapper in that we don't mock failing opcua connections etc.

	@Override
	public void fold(String shape, String orderId) {
		logger.info("fold called");
		wrapper.tell(FoldingMessageTypes.Fold, ActorRef.noSender());
	}

	@Override
	public void stop() {
		logger.info("stop called");
		wrapper.tell(FoldingMessageTypes.Stop, ActorRef.noSender());
	}

	@Override
	public void reset() {
		logger.info("reset called");
		wrapper.tell(FoldingMessageTypes.Reset, ActorRef.noSender());
	}

	@Override
	public void subscribeToStatus() {
		logger.info("subscribeToStatus called");
		wrapper.tell(FoldingMessageTypes.SubscribeState, ActorRef.noSender());
	}

}
