package shopfloor.agents.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import shopfloor.agents.messages.FrontEndMessages.OrderStatusRequest;
import shopfloor.agents.messages.FrontEndMessages.OrderStatusResponse;
import shopfloor.agents.messages.OrderStatus.JobStatus;
import shopfloor.agents.eventbus.OrderEventBus;
import shopfloor.agents.events.OrderEvent;
import shopfloor.agents.events.OrderEvent.OrderEventType;
import shopfloor.agents.messages.LockForOrder;
import shopfloor.agents.messages.NotifyAvailableForOrder;
import shopfloor.agents.messages.OrderDocument;
import shopfloor.agents.messages.OrderStatus;
import shopfloor.agents.messages.ProductionStateUpdate;
import shopfloor.agents.messages.ProductionStateUpdate.ProductionState;
import shopfloor.agents.messages.RegisterOrderRequest;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class OrderAgent extends AbstractActor{
	
	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	
	protected String orderId;
	protected List<String> jobList = new ArrayList<>();
	protected HashMap<String, ActorRef> job2machineDict = new HashMap<>();
	protected String currentJobId;	
	protected OrderStatus status;
	protected OrderEventBus eventBus;
	
	static public Props props(OrderDocument orderDoc, HashMap<String, ActorRef> job2machineDict, OrderEventBus eventBus) {	    
		return Props.create(OrderAgent.class, () -> new OrderAgent(orderDoc, job2machineDict, eventBus));
	  }
	
	// for now provide all details, later use registry service, and sophisticated process
	public OrderAgent(OrderDocument orderDoc, HashMap<String, ActorRef> job2machineDict,OrderEventBus eventBus) {
		this.orderId = orderDoc.getId();
		this.jobList = orderDoc.getJobs();
		this.job2machineDict = job2machineDict;
		this.status = new OrderStatus(orderDoc);
		eventBus.publish(new OrderEvent(this.orderId, OrderEventType.CREATED, this.status));
		this.eventBus = eventBus;
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
		        		status.setStatus(this.currentJobId, JobStatus.COMPLETED);
		        		eventBus.publish(new OrderEvent(this.orderId, OrderEventType.PRODUCTION_UPDATE, this.status));
		        		if (!jobList.isEmpty()) {
		        			String nextJobId = jobList.remove(0);
		        			job2machineDict.get(nextJobId).tell(new RegisterOrderRequest(nextJobId, null, getSelf()), getSelf());
		        		} else {
		        			eventBus.publish(new OrderEvent(this.orderId, OrderEventType.PRODUCTION_UPDATE, this.status));
		        		}
		        	}
		        	if (x.getStateReached().equals(ProductionState.STARTED)) {
		        		this.currentJobId = x.getJobId();
		        		status.setStatus(this.currentJobId, JobStatus.INPROGRESS);
		        		eventBus.publish(new OrderEvent(this.orderId, OrderEventType.COMPLETED, this.status));
		        	}
		        })
		        .match(OrderStatusRequest.class, req -> {
		        	sender().tell(Optional.of(new OrderStatusResponse(status)), getSelf());
		        })
		        .build();
	}

}
