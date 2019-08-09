package shopfloor.agents.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import shopfloor.agents.messages.LockForOrder;
import shopfloor.agents.messages.NotifyAvailableForOrder;
import shopfloor.agents.messages.ProductionStateUpdate;
import shopfloor.agents.messages.ProductionStateUpdate.ProductionState;
import shopfloor.agents.messages.RegisterOrderRequest;
import shopfloor.agents.messages.TransportRequest;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class OrderAgent extends AbstractActor{
	
	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	
	protected String orderId;
	protected List<String> jobList = new ArrayList<>();
	protected HashMap<String, ActorRef> job2machineDict = new HashMap<>();
	protected String currentJobId;	
	
	
	static public Props props(String orderId, List<String> jobList, HashMap<String, ActorRef> job2machineDict) {
	    return Props.create(OrderAgent.class, () -> new OrderAgent(orderId, jobList, job2machineDict));
	  }
	
	// for now provide all details, later use registry service, and sophisticated process
	public OrderAgent(String orderId, List<String> jobList, HashMap<String, ActorRef> job2machineDict) {
		this.orderId = orderId;
		this.jobList = jobList;
		this.job2machineDict = job2machineDict;
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder() // needs refactoring to put behavior into separate methods
		        .match(NotifyAvailableForOrder.class, nafo -> {
		        	log.info(nafo.getJobId());
		        	job2machineDict.get(nafo.getJobId()).tell(new LockForOrder(nafo.getJobId()), getSelf());
//		        	String nextJobId = nafo.getJobId();
//		        	String lastJobId = currentJobId;
//		        	// order transport
//		        	TransportRequest treq = new TransportRequest(nextJobId, job2machineDict.get(lastJobId), job2machineDict.get(nextJobId));
		        })
		        .match(ProductionStateUpdate.class, x -> {
		        	log.info(x.toString());
		        	if (x.getStateReached().equals(ProductionState.COMPLETED)) { // get next job, check if this message is really for previous job, we trust here that it is
		        		if (!jobList.isEmpty()) {
		        			String nextJobId = jobList.remove(0);
		        			job2machineDict.get(nextJobId).tell(new RegisterOrderRequest(nextJobId, null, getSelf()), getSelf());
		        		}
		        	}
		        	if (x.getStateReached().equals(ProductionState.STARTED)) {
		        		this.currentJobId = x.getJobId();
		        	}
		        })
		        .build();
	}

}
