package shopfloor.planer.actors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import ActorCoreModel.Actor;
import ProcessCore.CapabilityInvocation;
import ProcessCore.ProcessStep;
import actorprocess.ActorAllocation;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import shopfloor.agents.eventbus.OrderEventBus;
import shopfloor.agents.events.OrderBaseEvent;
import shopfloor.agents.events.OrderBaseEvent.OrderEventType;
import shopfloor.agents.events.OrderProcessUpdateEvent;
import shopfloor.agents.messages.LockForOrder;
import shopfloor.agents.messages.OrderDocument;
import shopfloor.agents.messages.OrderStatus;
import shopfloor.agents.messages.ReadyForProcessEvent;
import shopfloor.agents.messages.RegisterProcessRequest;
import shopfloor.agents.messages.RegisterProcessStepRequest;
import shopfloor.agents.messages.order.MappedOrderProcess;
import shopfloor.agents.messages.order.OrderProcess.ProcessChangeImpact;

public class OrderPlaningActor extends AbstractActor{

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

	protected Map<String, RegisterProcessRequest> reqIndex = new HashMap<>();
	protected List<MappedOrderProcess> orders = new ArrayList<MappedOrderProcess>();
	protected Map<AbstractActor, ActorRef> modelActors2AkkaActors = new HashMap<>();
	// dont make this bidirectional as we use them differently!!
	protected Map<ActorRef, String> machineWorksOnOrder = new HashMap<ActorRef, String>();
	protected Map<String, ActorRef> rootOrderAllocatedToMachine = new HashMap<String, ActorRef>();

	protected OrderEventBus eventBus;

	static public Props props(OrderDocument orderDoc, HashMap<String, ActorRef> job2machineDict, OrderEventBus eventBus) {	    
		return Props.create(OrderPlaningActor.class, () -> new OrderPlaningActor( eventBus));
	}

	public OrderPlaningActor(OrderEventBus eventBus) {
		// obtain info on available machines/actors
		// obtain reference to transport subsystem actor
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(RegisterProcessRequest.class, rpReq -> {
					log.info("Received Register Order Request");		        	
					reqIndex.put(rpReq.getRootOrderId(), rpReq);
					eventBus.publish(new OrderBaseEvent(rpReq.getRootOrderId(), this.self().path().name(), OrderEventType.REGISTERED));
					if (rpReq.getProcess() instanceof MappedOrderProcess) { 
						scheduleProcess(rpReq.getRootOrderId(), (MappedOrderProcess) rpReq.getProcess());		        		
					} else {
						mapProcessToMachines(rpReq);
					}		        	
				}) 
				.match(ReadyForProcessEvent.class, readyE -> {
					produceProcessAtMachine(readyE);
				})
				.match(OrderBaseEvent.class, orderEvent -> {
					// check for order transport progress
				})
				.match(MachineBaseEvent.class, machineEvent -> {
					// check for machine production progress on order
					// check for machine availability for an order
					// collect machine as actors on the shopfloor
				})
				.match(TransportOrderResponse.class, transportResp -> {
					
				})
				.build();
	}


	private void mapProcessToMachines(RegisterProcessRequest rpReq) {
		log.warning(String.format("OrderProcess %s has no mapped Actors based on Capabilities, this is not supported yet",rpReq.getRootOrderId()));
		eventBus.publish(new OrderBaseEvent(rpReq.getRootOrderId(), this.self().path().name(), OrderEventType.CANCELED));
		throw new RuntimeException("Not Implemented yet");		
	}

	private void scheduleProcess(String rootOrderId, MappedOrderProcess mop) {
		ProcessChangeImpact pci = mop.activateProcess();
		eventBus.publish( new OrderProcessUpdateEvent(rootOrderId, this.self().path().name(), pci) );

		// basically we get possible steps from process (filter out flow control elements) and register first step at machine
		List<CapabilityInvocation> stepCandidates = mop.getAvailableSteps().stream()
				.filter(step ->(step instanceof CapabilityInvocation) )
				.map(CapabilityInvocation.class::cast)
				.collect(Collectors.toList());
		if (stepCandidates.isEmpty()) {
			log.warning(String.format("OrderProcess %s has no available steps of type CapabilityInvocation to start with",rootOrderId));
			eventBus.publish(new OrderBaseEvent(rootOrderId, this.self().path().name(), OrderEventType.CANCELED));			
		} else {
			orders.add(mop);
			assignExecutingMachineForProcessStep(stepCandidates, mop, rootOrderId);
		}
	}

	private void assignExecutingMachineForProcessStep(List<CapabilityInvocation> stepCandidates, MappedOrderProcess mop, String rootOrderId) {
		// find one mapped machines if any machine is available at the moment, implement JIT assignment
		List<ActorAllocation> aaOpts = stepCandidates.stream()
				.map(cap -> mop.getActorAllocationForProcess(cap))
				.filter(allocOpt -> allocOpt.isPresent())
				.map(allocOpt -> allocOpt.get()) // here we have mapped steps,
				.collect(Collectors.toList());
		if (aaOpts.isEmpty()) {
			log.warning(String.format("OrderProcess %s has no available CapabilityInvocations steps match a discovered Machine/Actor, Order is paused",rootOrderId));
			eventBus.publish(new OrderBaseEvent(rootOrderId, this.self().path().name(), OrderEventType.PAUSED));
		} else {

			Optional<ActorAllocation> aaOpt = aaOpts.stream()
					.filter(alloc -> modelActors2AkkaActors.containsKey(alloc.getActor()) ) // we have a mapping to an existing machine
					.filter(alloc -> machineWorksOnOrder.containsKey(modelActors2AkkaActors.containsKey(alloc.getActor())) ) // now we keep those where a machine is available (i.e., not working on something
					.findFirst();
			aaOpt.ifPresent(aa -> {
				// get machine actor reference
				ActorRef machine = modelActors2AkkaActors.get(aa.getActor());
				machine.tell( new RegisterProcessStepRequest(rootOrderId, aa.getAllocatedTo().toString(), aa.getAllocatedTo(), this.self()), this.self());
				rootOrderAllocatedToMachine.put(rootOrderId, machine); // allocated, not yet producing
				//TODO: alternatively the machine could issue a SCHEDULED event
				eventBus.publish(new OrderBaseEvent(rootOrderId, this.self().path().name(), OrderEventType.SCHEDULED));
			});
			if (!aaOpt.isPresent()) {
				log.info(String.format("OrderProcess %s has no available CapabilityInvocations steps match an available Machine/Actor, Order is paused",rootOrderId));
				eventBus.publish(new OrderBaseEvent(rootOrderId, this.self().path().name(), OrderEventType.PAUSED));
			}
		}
	}

	private void produceProcessAtMachine(ReadyForProcessEvent readyE) {
		if (readyE.isReady()) {		
			// TODO: theoretically we would need to check if we canceled the order in the meantime, then we wont allocated that order but rather free up the machine
			machineWorksOnOrder.put(this.getSender(), readyE.getResponseTo().getRootOrderId());
			this.getSender().tell(new LockForOrder(readyE.getResponseTo().getProcessStepId(), readyE.getResponseTo().getRootOrderId()), this.getSelf());
			eventBus.publish(new OrderBaseEvent(readyE.getResponseTo().getRootOrderId(), this.self().path().name(), OrderEventType.ALLOCATED));
		} else { // e.g., when machine needs to go down for maintenance, or some other error occured in the meantime
			rootOrderAllocatedToMachine.remove(readyE.getResponseTo().getRootOrderId());
			// TODO: what shall we do then, check again for that machine's availability?!
		}
	}

	private void handleNoLongerAvailableMachine() {
		// check if it was producing anything
		// if so, where that product/pallet/order currently is
		// check if it was allocated to produce something
		// if so, undo order allocation
	}
	
	private void handleNewlyAvailableMachine() {
		// register, wait for updates
	}
	
	private void handleProductionStartingEvent() {
		// not much
	}
	
	private void handleProductionCompletionEvent() {
		// check which next machine to produce on. if none, keep pallet/order on machine,
		// of non available either wait for machine to be ready, or preallocate to machine, free up order, but keep machine mapped
		// TODO: check if that makes sense
	}
	
	private void handleMachineIdleStateEvent() {
		// implies machine is empty
		
	}
	
	private void handleResponseToTransportRequest() {
		
	}
	
}
