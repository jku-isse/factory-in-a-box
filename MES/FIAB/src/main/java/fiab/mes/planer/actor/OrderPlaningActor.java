package fiab.mes.planer.actor;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import fiab.mes.eventbus.OrderEventBus;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.machine.msg.MachineDisconnectedEvent;
import fiab.mes.machine.msg.MachineUpdateEvent;
import fiab.mes.order.MappedOrderProcess;
import fiab.mes.order.OrderProcess;
import fiab.mes.order.OrderProcess.ProcessChangeImpact;
import fiab.mes.order.msg.LockForOrder;
import fiab.mes.order.msg.OrderEvent;
import fiab.mes.order.msg.OrderEvent.OrderEventType;
import fiab.mes.order.msg.OrderProcessUpdateEvent;
import fiab.mes.order.msg.ReadyForProcessEvent;
import fiab.mes.order.msg.RegisterProcessRequest;
import fiab.mes.order.msg.RegisterProcessStepRequest;


public class OrderPlaningActor extends AbstractActor{

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

	protected Map<String, RegisterProcessRequest> reqIndex = new HashMap<>();
	protected List<MappedOrderProcess> orders = new ArrayList<MappedOrderProcess>();
	protected Map<AbstractActor, ActorRef> modelActors2AkkaActors = new HashMap<>();
	// dont make this bidirectional as we use them differently!!
	//protected Map<ActorRef, String> machineWorksOnOrder = new HashMap<ActorRef, String>();
	//protected Map<String, ActorRef> rootOrderAllocatedToMachine = new HashMap<String, ActorRef>();

	protected Map<Entry<ActorRef, String>, OrderEventType> scheduleStatus = new HashMap<>();
	
	protected OrderEventBus eventBus;

	static public Props props() {	    
		return Props.create(OrderPlaningActor.class, () -> new OrderPlaningActor());
	}

	public OrderPlaningActor() {
		
		// obtain info on available machines/actors
		// obtain reference to transport subsystem actor
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(RegisterProcessRequest.class, rpReq -> {
					log.info("Received Register Order Request");		        	
					reqIndex.put(rpReq.getRootOrderId(), rpReq);
					eventBus.publish(new OrderEvent(rpReq.getRootOrderId(), this.self().path().name(), OrderEventType.REGISTERED));
					if (rpReq.getProcess() instanceof MappedOrderProcess) { 
						scheduleProcess(rpReq.getRootOrderId(), (MappedOrderProcess) rpReq.getProcess());		        		
					} else {
						mapProcessToMachines(rpReq);
					}		        	
				}) 
				.match(ReadyForProcessEvent.class, readyE -> {
					produceProcessAtMachine(readyE);
				})
				.match(OrderEvent.class, orderEvent -> {
					// check for order transport progress
				})
				.match(MachineConnectedEvent.class, machineEvent -> {
					handleNewlyAvailableMachine(machineEvent);
					// check for machine production progress on order
					// collect machine as actors on the shopfloor
				})
				.match(MachineDisconnectedEvent.class, machineEvent -> {
					handleNoLongerAvailableMachine(machineEvent);
					// check for machine production progress on order
					// collect machine as actors on the shopfloor
				})
				.match(MachineUpdateEvent.class, machineEvent -> {
					handleMachineUpdateEvent(machineEvent);
				})
				.match(TransportOrderResponse.class, transportResp -> {
					
				})
				.build();
	}


	private void mapProcessToMachines(RegisterProcessRequest rpReq) {
		log.warning(String.format("OrderProcess %s has no mapped Actors based on Capabilities, this is not supported yet",rpReq.getRootOrderId()));
		eventBus.publish(new OrderEvent(rpReq.getRootOrderId(), this.self().path().name(), OrderEventType.CANCELED));
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
			eventBus.publish(new OrderEvent(rootOrderId, this.self().path().name(), OrderEventType.CANCELED));			
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
			eventBus.publish(new OrderEvent(rootOrderId, this.self().path().name(), OrderEventType.PAUSED));
		} else {

			Optional<ActorAllocation> aaOpt = aaOpts.stream()
					.filter(alloc -> modelActors2AkkaActors.containsKey(alloc.getActor()) ) // we have a mapping to an existing machine
					.filter(alloc -> scheduleStatus.containsKey(toTuple(modelActors2AkkaActors.get(alloc.getActor()),rootOrderId)) ) // now we keep those where a machine is available (i.e., not working on something
					.findFirst();
			aaOpt.ifPresent(aa -> {
				// get machine actor reference
				ActorRef machine = modelActors2AkkaActors.get(aa.getActor());
				machine.tell( new RegisterProcessStepRequest(rootOrderId, aa.getAllocatedTo().toString(), aa.getAllocatedTo(), this.self()), this.self());
				//rootOrderAllocatedToMachine.put(rootOrderId, machine); // allocated, not yet producing
				scheduleStatus.put(toTuple(machine,rootOrderId), OrderEventType.SCHEDULED);
				//TODO: alternatively the machine could issue a SCHEDULED event
				eventBus.publish(new OrderEvent(rootOrderId, this.self().path().name(), OrderEventType.SCHEDULED));
			});
			if (!aaOpt.isPresent()) {
				log.info(String.format("OrderProcess %s has no available CapabilityInvocations steps match an available Machine/Actor, Order is paused",rootOrderId));
				eventBus.publish(new OrderEvent(rootOrderId, this.self().path().name(), OrderEventType.PAUSED));
			}
		}
	}

	private void produceProcessAtMachine(ReadyForProcessEvent readyE) {
		if (readyE.isReady()) {		
			// TODO: theoretically we would need to check if we canceled the order in the meantime, then we wont allocated that order but rather free up the machine
			//machineWorksOnOrder.put(this.getSender(), readyE.getResponseTo().getRootOrderId());
			scheduleStatus.put(toTuple(this.getSender(),readyE.getResponseTo().getRootOrderId()), OrderEventType.ALLOCATED);
			this.getSender().tell(new LockForOrder(readyE.getResponseTo().getProcessStepId(), readyE.getResponseTo().getRootOrderId()), this.getSelf());
			eventBus.publish(new OrderEvent(readyE.getResponseTo().getRootOrderId(), this.self().path().name(), OrderEventType.ALLOCATED));
		} else { // e.g., when machine needs to go down for maintenance, or some other error occured in the meantime
			//rootOrderAllocatedToMachine.remove(readyE.getResponseTo().getRootOrderId());
			scheduleStatus.remove(toTuple(this.getSender(),readyE.getResponseTo().getRootOrderId()));
			// TODO: what shall we do then, check again for that machine's availability?!
		}
	}

	private void handleNoLongerAvailableMachine(MachineDisconnectedEvent mde) {
		// check if it was producing anything
		// if so, where that product/pallet/order currently is
		// check if it was allocated to produce something
		// if so, undo order allocation
	}
	
	private void handleNewlyAvailableMachine(MachineConnectedEvent mce) {
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
	
	private void handleMachineUpdateEvent(MachineUpdateEvent mue) {
		// if state changes to idle
		// implies machine is empty
		
		
	}
	
	private void handleResponseToTransportRequest() {
		
	}
	
	private Entry<ActorRef, String> toTuple(ActorRef machine, String rootProcessId) {
		return new AbstractMap.SimpleEntry<ActorRef, String>(machine, rootProcessId);
	}
}
