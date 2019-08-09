package shopfloor.agents.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import akka.actor.AbstractActor;
import akka.actor.Props;
import shopfloor.agents.messages.LockForOrder;
import shopfloor.agents.messages.NotifyAvailableForOrder;
import shopfloor.agents.messages.ProductionStateUpdate;
import shopfloor.agents.messages.ProductionStateUpdate.ProductionState;
import shopfloor.agents.messages.RegisterOrderRequest;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class MachineAgent extends AbstractActor{

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	
	protected String machineId;
	protected List<RegisterOrderRequest> orderList = new ArrayList<>();
	protected LockForOrder currentJob = null;
	
	static public Props props(String id) {
	    return Props.create(MachineAgent.class, () -> new MachineAgent(id));
	  }
	
	public MachineAgent(String id) {
		this.machineId = id;
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
		        .match(LockForOrder.class, lfo -> {
		        	log.info(lfo.getJobId());
		        	if (currentJob == null) { // we don't allow to override another order
		        		currentJob = lfo;
		        		// send machine configuration, resp ask machine to configure for next job
		        		// here for mocking we immediatly send started and completed events
		        		getSender().tell(new ProductionStateUpdate(ProductionState.STARTED, lfo.getJobId(), Instant.now()), getSelf());
		        		currentJob = null;
		        		getSender().tell(new ProductionStateUpdate(ProductionState.COMPLETED, lfo.getJobId(), Instant.now()), getSelf());
		        	}
		        })
		        .match(RegisterOrderRequest.class, x -> {
		        	log.info(x.getJobId());
		        	orderList.add(x);
		        	if (currentJob == null && !orderList.isEmpty()) { // if we are idle, tell next order to get ready, this logic is also triggered upon machine signaling completion
		        		RegisterOrderRequest ror = orderList.remove(0);
		        		ror.getOrderAgent().tell(new NotifyAvailableForOrder(ror.getJobId()), getSelf());
		        	}		        		
		        })
		        .build();
	}

	// todo: all the internal communication with the actual machine via OPC-UA
	
}
