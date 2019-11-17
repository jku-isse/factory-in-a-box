//<<<<<<< Updated upstream
//package fiab.mes.planer.actor;
//
//import java.util.AbstractMap;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Map.Entry;
//import java.util.Optional;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//import ActorCoreModel.Actor;
//import ProcessCore.AbstractCapability;
//import ProcessCore.CapabilityInvocation;
//import ProcessCore.ParallelBranches;
//import ProcessCore.ProcessStep;
//import actorprocess.ActorAllocation;
//import akka.actor.AbstractActor;
//import akka.actor.ActorRef;
//import akka.actor.ActorSelection;
//import akka.actor.Props;
//import akka.actor.RootActorPath;
//import akka.event.Logging;
//import akka.event.LoggingAdapter;
//import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
//import fiab.mes.eventbus.OrderEventBus;
//import fiab.mes.eventbus.OrderEventBusWrapperActor;
//import fiab.mes.eventbus.SubscribeMessage;
//import fiab.mes.eventbus.SubscriptionClassifier;
//import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
//import fiab.mes.machine.msg.MachineConnectedEvent;
//import fiab.mes.machine.msg.MachineDisconnectedEvent;
//import fiab.mes.machine.msg.MachineUpdateEvent;
//import fiab.mes.order.MappedOrderProcess;
//import fiab.mes.order.OrderProcess;
//import fiab.mes.order.OrderProcess.ProcessChangeImpact;
//import fiab.mes.order.msg.LockForOrder;
//import fiab.mes.order.msg.OrderEvent;
//import fiab.mes.order.msg.OrderEvent.OrderEventType;
//import fiab.mes.order.msg.OrderProcessUpdateEvent;
//import fiab.mes.order.msg.ReadyForProcessEvent;
//import fiab.mes.order.msg.RegisterProcessRequest;
//import fiab.mes.order.msg.RegisterProcessStepRequest;
//import fiab.mes.planer.actor.MachineOrderMappingManager.MachineOrderMappingStatus;
//
//
//public class OrderPlanningActor extends AbstractActor{
//
//	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
//
//	public static final String WELLKNOWN_LOOKUP_NAME = "DefaultOrderPlanningActor";
//	
//	//protected Map<String, RegisterProcessRequest> reqIndex = new HashMap<>();
//	//protected List<MappedOrderProcess> orders = new ArrayList<MappedOrderProcess>();
//	
//	// manages which machine has which capabilities
//	protected MachineCapabilityManager capMan = new MachineCapabilityManager();
//	
//	// manages which machines has currently which order allocated and in which state
//	protected MachineOrderMappingManager ordMapper;
//	
//	
//	
//	//protected Map<ActorRef, List<AbstractCapability>> capabilities = new HashMap<>();
//	//protected Map<Actor, ActorRef> modelActors2AkkaActors = new HashMap<>();
//	// dont make this bidirectional as we use them differently!!
//	//protected Map<ActorRef, String> machineWorksOnOrder = new HashMap<ActorRef, String>();
//	//protected Map<String, ActorRef> rootOrderAllocatedToMachine = new HashMap<String, ActorRef>();
//	//protected Map<Entry<AkkaActorBackedCoreModelAbstractActor, String>, OrderEventType> scheduleStatus = new HashMap<>();
//	
//	protected ActorSelection orderEventBus; 	
//	protected ActorSelection machineEventBus;
//
//	static public Props props() {	    
//		return Props.create(OrderPlanningActor.class, () -> new OrderPlanningActor());
//	}
//
//	public OrderPlanningActor() {
//		getEventBusAndSubscribe();
//		
//		// obtain info on available machines/actors
//		// obtain reference to transport subsystem actor
//	}
//
//	@Override
//	public Receive createReceive() {
//		return receiveBuilder()
//				.match(RegisterProcessRequest.class, rpReq -> {
//					log.info("Received Register Order Request");		        	
//					//reqIndex.put(rpReq.getRootOrderId(), rpReq);
//					ordMapper.registerOrder(rpReq);
//					scheduleProcess(rpReq.getRootOrderId(), rpReq.getProcess());		        		
//				}) 
//				.match(ReadyForProcessEvent.class, readyE -> {
//					produceProcessAtMachine(readyE);
//				})
//				.match(OrderEvent.class, orderEvent -> {					
//					// to confirm that an order is now produced or completed at a machine
//				})
//				.match(MachineConnectedEvent.class, machineEvent -> {
//					handleNewlyAvailableMachine(machineEvent);
//				})
//				.match(MachineDisconnectedEvent.class, machineEvent -> {
//					handleNoLongerAvailableMachine(machineEvent);
//				})
//				.match(MachineUpdateEvent.class, machineEvent -> {
//					handleMachineUpdateEvent(machineEvent);
//				})
////				.match(TransportOrderResponse.class, transportResp -> {
////					
////				})
//				.build();
//	}
//
//	private void getEventBusAndSubscribe() {
//		SubscribeMessage orderSub = new SubscribeMessage(getSelf(), new SubscriptionClassifier(WELLKNOWN_LOOKUP_NAME, "*"));		
//		orderEventBus = this.context().actorSelection("/user/"+OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
//		orderEventBus.tell(orderSub, getSelf());
//		
//		SubscribeMessage machineSub = new SubscribeMessage(getSelf(), new SubscriptionClassifier(WELLKNOWN_LOOKUP_NAME, "*"));
//		machineEventBus = this.context().actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
//		machineEventBus.tell(machineSub, getSelf());
//		
//		ordMapper = new MachineOrderMappingManager(orderEventBus, self().path().name());
//	}
//	
////	private void mapProcessToMachines(RegisterProcessRequest rpReq) {
////		log.warning(String.format("OrderProcess %s has no mapped Actors based on Capabilities, this is not supported yet",rpReq.getRootOrderId()));
////		orderEventBus.tell(new OrderEvent(rpReq.getRootOrderId(), this.self().path().name(), OrderEventType.CANCELED), ActorRef.noSender());
////		// check if every step can be mapped to a machine (for now we assume there is only 
////		throw new RuntimeException("Not Implemented yet");		
////	}
//
//	// first time activation of the process
//	private void scheduleProcess(String rootOrderId, OrderProcess mop) {
//		if (!mop.doAllLeafNodeStepsHaveInvokedCapability(mop.getProcess())) {
//			log.warning(String.format("OrderProcess %s does not have all leaf nodes with capability invocations, thus cannot be completely mapped to machines, cancelling order", rootOrderId));
//			ordMapper.removeOrder(rootOrderId); // we didn/t start processing yet, so we can just drop the process
//			orderEventBus.tell(new OrderEvent(rootOrderId, this.self().path().name(), OrderEventType.CANCELED), ActorRef.noSender());
//		} else {
//			// we have capability invocations with capabilities, thus able to map them to machines
//			ProcessChangeImpact pci = mop.activateProcess();
//			orderEventBus.tell( new OrderProcessUpdateEvent(rootOrderId, this.self().path().name(), pci), ActorRef.noSender() );
//			tryAssignExecutingMachineForOneProcessStep(mop, rootOrderId);
//		} 
//	}
//
//	
//	// this method will trigger order to machine allocation when there is a machine already idle,
//	// if there are all machines occupied, then this will not do anything (order will be paused)
//	private void tryAssignExecutingMachineForOneProcessStep(OrderProcess op, String rootOrderId) {
//		// basically we get possible steps from process (filter out flow control elements) and register first step at machine
//		List<CapabilityInvocation> stepCandidates = op.getAvailableSteps().stream()
//				.filter(step ->(step instanceof CapabilityInvocation) )
//				.map(CapabilityInvocation.class::cast)
//				.filter(capInv -> capInv.getInvokedCapability() != null)
//				.collect(Collectors.toList());
//		if (stepCandidates.isEmpty()) {
//			// check if process is finished, then we can remove it from the shopfloor 
//			if (op.areAllTasksCancelledOrCompleted()) {
//				// no need to signal completion here, as step level completion already happended, and completion only when process arrives at output station
//				log.info(String.format("OrderProcess %s is complete, now triggering move to output station",rootOrderId));
//				//TODO: --> transport it off to output station
//			} else {
//				// this implies that the process cant make progress as there is no available capabilityinvocation ie. process step to allocate to a machine
//				log.warning(String.format("OrderProcess %s has no available steps of type CapabilityInvocation to continue with",rootOrderId));
//				orderEventBus.tell(new OrderEvent(rootOrderId, this.self().path().name(), OrderEventType.CANCELED), ActorRef.noSender());		
//				//TODO: if this process is somewhere on some machine, remove it from production --> transport it off to output station
//			}
//		} else {
//			// go through each candidate and check if any of the steps can be mapped to a available machine,
//			// first get candidates where the machine capabilites matches
//			Map<CapabilityInvocation,Set<AkkaActorBackedCoreModelAbstractActor>> capMap = stepCandidates.stream()
//				.map(step -> new AbstractMap.SimpleEntry<CapabilityInvocation,Set<AkkaActorBackedCoreModelAbstractActor>>(step, capMan.getMachinesProvidingCapability(step.getInvokedCapability())) )
//				.filter(pair -> !pair.getValue().isEmpty() )
//				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (prev, next) -> next, HashMap::new));
//				
//			
//			// select all candidate steps that have some actor allocation to modelActors 						
////			List<ActorAllocation> aaOpts = stepCandidates.stream()
////					.map(cap -> op.getActorAllocationForProcess(cap))
////					.filter(allocOpt -> allocOpt.isPresent())
////					.map(allocOpt -> allocOpt.get()) // here we have mapped steps,
////					.collect(Collectors.toList());
//			if (capMap.isEmpty()) {
//				log.warning(String.format("OrderProcess %s has no available CapabilityInvocations steps match a discovered Machine/Actor, Order is paused",rootOrderId));
//				ordMapper.pauseOrder(rootOrderId);
//			} else { // among those with available capabilities, select first where there is a machine mapped and available at the moment, implement JIT assignment
//				
//				// for each step produce tuple list of available machines
//				Optional<AbstractMap.SimpleEntry<CapabilityInvocation, AkkaActorBackedCoreModelAbstractActor>> maOpt = capMap.entrySet().stream()
//						.flatMap(pair -> pair.getValue().stream().map(mach -> new AbstractMap.SimpleEntry<>(pair.getKey(), mach)))
//						.filter(flatPair -> flatPair.getValue() != null)
//						.findFirst(); // then from this list pick the first
//				
////						.map(alloc -> new AbstractMap.SimpleEntry<>(alloc, capMan.resolveByModelActor(alloc.getActor())) )
////						.filter(allocMap -> allocMap.getValue().isPresent())
////						.map(allocMap -> new AbstractMap.SimpleEntry<>(allocMap.getKey(), allocMap.getValue().get()))					
////						.filter(allocMap -> ordMapper.isMachineIdle(allocMap.getValue()) ) // we have an idle machine 
////						.findFirst();
//				
//				maOpt.ifPresent(aa -> {								
//					// scheduled, not yet producing
//					ordMapper.requestMachineForOrder(aa.getValue(), rootOrderId, aa.getKey());	
//					// get machine actor reference				
//					aa.getValue().getAkkaActor().tell( new RegisterProcessStepRequest(rootOrderId, aa.getKey().toString(), aa.getKey(), this.self()), this.self());					
//					//TODO: alternatively the machine could issue a SCHEDULED event
//					ordMapper.scheduleProcess(rootOrderId);
//				});
//				if (!maOpt.isPresent()) {
//					log.info(String.format("OrderProcess %s has no available CapabilityInvocations steps that match an available Machine/Actor, Order is paused",rootOrderId));
//					ordMapper.pauseOrder(rootOrderId);
//				}
//			}
//		}
//	}
//
//	private void produceProcessAtMachine(ReadyForProcessEvent readyE) {
//		String orderId = readyE.getResponseTo().getRootOrderId();
//		capMan.resolveByAkkaActor(getSender()).ifPresent(machine -> {
//			if (readyE.isReady()) {		
//				// TODO: theoretically we would need to check if we canceled the order in the meantime, then we wont allocated that order but rather free up the machine
//				ordMapper.reserveOrderAtMachine(machine);			
//				this.getSender().tell(new LockForOrder(readyE.getResponseTo().getProcessStepId(), orderId), this.getSelf());
//				//upon lockfororder,the machine should transition into starting state
//				//TODO this should actually be published by the machine
//				ordMapper.allocateProcess(orderId);
//				// request transport to that machine: this machine should now be in Starting state
//				requestTransport(machine, orderId);	
//				// update that we now work on an order
//				ordMapper.getOrderRequest(orderId).ifPresent(rpr -> {
//					ProcessChangeImpact pci = rpr.getProcess().activateStepIfAllowed(readyE.getResponseTo().getProcessStep());
//					orderEventBus.tell( new OrderProcessUpdateEvent(orderId, this.self().path().name(), pci), ActorRef.noSender() );
//				});
//				
//			} else { // e.g., when machine needs to go down for maintenance, or some other error occured in the meantime						
//				ordMapper.freeUpMachine(machine);			
//				ordMapper.pauseOrder(readyE.getResponseTo().getRootOrderId());
//				// then Wait for event on status update
//			}
//		});
//	}
//	
//	private void handleNoLongerAvailableMachine(MachineDisconnectedEvent mde) {		
//		capMan.removeActor(mde.getMachine());
//		Optional<MachineOrderMappingStatus> momStatus = ordMapper.removeMachine(mde.getMachine());
//		momStatus.ifPresent(mom -> {
//			// check if it was producing anything
//			if (mom.getOrderId() == null) return;			
//			// TODO: if so, where that product/pallet/order currently is
//			// check if it was allocated to produce something
//			// if so, undo order allocation	
//		});
//		
//	}
//	
//	private void handleNewlyAvailableMachine(MachineConnectedEvent mce) {
//		capMan.setCapabilities(mce);
//		log.info("Storing Capabilities for machine: "+mce.getMachineId());
//		// now wait for machine available event to make use of it (currently we dont know its state)				
//	}
//	
//	private void handleProductionCompletionEvent() {
//		// just complete the processstep in the process if not already done so
//		
//	}
//	
//	private void handleMachineUpdateEvent(MachineUpdateEvent mue) {
//		log.info(String.format("MachineUpdateEvent for machine %s %s : %s", mue.getMachineId(), mue.getType(), mue.getNewValue().toString()));
//		capMan.resolveById(mue.getMachineId()).ifPresent(machine -> {
//			// will only process event if the parameter changes is "STATE"
//			ordMapper.updateMachineStatus(machine, mue);
//			if (mue.getType().equals(MachineOrderMappingManager.STATE_VAR_NAME)) {
//				if (mue.getNewValue().equals(MachineOrderMappingManager.IDLE_STATE_VALUE)) {
//					// if idle --> machine ready --> lets check if any order is waiting for that machine
//					ordMapper.getPausedProcessesOnSomeMachine().stream()
//						.forEach(rpr -> tryAssignExecutingMachineForOneProcessStep(rpr.getProcess(), rpr.getRootOrderId()));
//					// we don't abort upon first success but try for every one, should matter, just takes a bit of processing
//					//if none, then just wait for next incoming event
//				} else if (mue.getNewValue().equals(MachineOrderMappingManager.COMPLETING_STATE_VALUE)) {
//					// TODO: step done, now update the process, so which step has been completed?
//					// we cannot rely on ProductionCompletionevent as there could be a race condition which event arrives first
//					
//					// then we still have the order occupying the machine  --> now check if next place is ready somewhere
//					// update process, get next step --> check if possible somewhere
//					ordMapper.getOrderRequestOnMachine(machine).ifPresent(rpr -> { 
//						 ordMapper.getJobOnMachine(machine).ifPresent(step -> { 
//							 ProcessChangeImpact pci = rpr.getProcess().markStepComplete(step); 
//							 orderEventBus.tell( new OrderProcessUpdateEvent(rpr.getRootOrderId(), this.self().path().name(), pci), ActorRef.noSender() );
//							 } );
//						tryAssignExecutingMachineForOneProcessStep(rpr.getProcess(), rpr.getRootOrderId()); });
//				} else if (mue.getNewValue().equals(MachineOrderMappingManager.PRODUCING_STATE_VALUE)) {
//					// now update mapping that order has arrived at that machine and is loaded
//					ordMapper.confirmOrderAtMachine(machine);
//				}
//			}						
//		});		
//		
//		
//	}
//	
//	private void requestTransport(AkkaActorBackedCoreModelAbstractActor destination, String orderId) {
//		
//		Optional<AkkaActorBackedCoreModelAbstractActor> currentLoc = ordMapper.getCurrentMachineOfOrder(orderId);
//		if (!currentLoc.isPresent()) {
//			log.warning("Unable to located current machine of order: "+orderId);
//		} else {
//			
//		}
//		
//	}
//	
//	private void handleResponseToTransportRequest() {
//		
//	}
//	
//
//	
//
//	
//	
//}
//=======
package fiab.mes.planer.actor;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import ActorCoreModel.Actor;
import ProcessCore.AbstractCapability;
import ProcessCore.CapabilityInvocation;
import ProcessCore.ParallelBranches;
import ProcessCore.ProcessStep;
import actorprocess.ActorAllocation;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.actor.RootActorPath;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.OrderEventBus;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.eventbus.SubscriptionClassifier;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
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
import fiab.mes.planer.actor.MachineOrderMappingManager.MachineOrderMappingStatus;


public class OrderPlanningActor extends AbstractActor{

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

	public static final String WELLKNOWN_LOOKUP_NAME = "DefaultOrderPlanningActor";
	
	//protected Map<String, RegisterProcessRequest> reqIndex = new HashMap<>();
	//protected List<MappedOrderProcess> orders = new ArrayList<MappedOrderProcess>();
	
	// manages which machine has which capabilities
	protected MachineCapabilityManager capMan = new MachineCapabilityManager();
	
	// manages which machines has currently which order allocated and in which state
	protected MachineOrderMappingManager ordMapper;
	
	
	
	//protected Map<ActorRef, List<AbstractCapability>> capabilities = new HashMap<>();
	//protected Map<Actor, ActorRef> modelActors2AkkaActors = new HashMap<>();
	// dont make this bidirectional as we use them differently!!
	//protected Map<ActorRef, String> machineWorksOnOrder = new HashMap<ActorRef, String>();
	//protected Map<String, ActorRef> rootOrderAllocatedToMachine = new HashMap<String, ActorRef>();
	//protected Map<Entry<AkkaActorBackedCoreModelAbstractActor, String>, OrderEventType> scheduleStatus = new HashMap<>();
	
	protected ActorSelection orderEventBus; 	
	protected ActorSelection machineEventBus;

	static public Props props() {	    
		return Props.create(OrderPlanningActor.class, () -> new OrderPlanningActor());
	}

	public OrderPlanningActor() {
		getEventBusAndSubscribe();
		
		// obtain info on available machines/actors
		// obtain reference to transport subsystem actor
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(RegisterProcessRequest.class, rpReq -> {
					log.info("Received Register Order Request");		        	
					//reqIndex.put(rpReq.getRootOrderId(), rpReq);
					ordMapper.registerOrder(rpReq);
					scheduleProcess(rpReq.getRootOrderId(), rpReq.getProcess());		        		
				}) 
				.match(ReadyForProcessEvent.class, readyE -> {
					produceProcessAtMachine(readyE);
				})
				.match(OrderEvent.class, orderEvent -> {					
					// to confirm that an order is now produced or completed at a machine
				})
				.match(MachineConnectedEvent.class, machineEvent -> {
					handleNewlyAvailableMachine(machineEvent);
				})
				.match(MachineDisconnectedEvent.class, machineEvent -> {
					handleNoLongerAvailableMachine(machineEvent);
				})
				.match(MachineUpdateEvent.class, machineEvent -> {
					handleMachineUpdateEvent(machineEvent);
				})
//				.match(TransportOrderResponse.class, transportResp -> {
//					
//				})
				.build();
	}

	private void getEventBusAndSubscribe() {
		SubscribeMessage orderSub = new SubscribeMessage(getSelf(), new SubscriptionClassifier(WELLKNOWN_LOOKUP_NAME, "*"));		
		orderEventBus = this.context().actorSelection("/user/"+OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		orderEventBus.tell(orderSub, getSelf());
		
		SubscribeMessage machineSub = new SubscribeMessage(getSelf(), new SubscriptionClassifier(WELLKNOWN_LOOKUP_NAME, "*"));
		machineEventBus = this.context().actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		machineEventBus.tell(machineSub, getSelf());
		
		ordMapper = new MachineOrderMappingManager(orderEventBus, self().path().name());
	}
	
//	private void mapProcessToMachines(RegisterProcessRequest rpReq) {
//		log.warning(String.format("OrderProcess %s has no mapped Actors based on Capabilities, this is not supported yet",rpReq.getRootOrderId()));
//		orderEventBus.tell(new OrderEvent(rpReq.getRootOrderId(), this.self().path().name(), OrderEventType.CANCELED), ActorRef.noSender());
//		// check if every step can be mapped to a machine (for now we assume there is only 
//		throw new RuntimeException("Not Implemented yet");		
//	}

	// first time activation of the process
	private void scheduleProcess(String rootOrderId, OrderProcess mop) {
		if (!mop.doAllLeafNodeStepsHaveInvokedCapability(mop.getProcess())) {
			log.warning(String.format("OrderProcess %s does not have all leaf nodes with capability invocations, thus cannot be completely mapped to machines, cancelling order", rootOrderId));
			ordMapper.removeOrder(rootOrderId); // we didn/t start processing yet, so we can just drop the process
			orderEventBus.tell(new OrderEvent(rootOrderId, this.self().path().name(), OrderEventType.CANCELED), ActorRef.noSender());
		} else {
			// we have capability invocations with capabilities, thus able to map them to machines
			ProcessChangeImpact pci = mop.activateProcess();
			orderEventBus.tell( new OrderProcessUpdateEvent(rootOrderId, this.self().path().name(), pci), ActorRef.noSender() );
			tryAssignExecutingMachineForOneProcessStep(mop, rootOrderId);
		} 
	}

	
	// this method will trigger order to machine allocation when there is a machine already idle,
	// if there are all machines occupied, then this will not do anything (order will be paused)
	private void tryAssignExecutingMachineForOneProcessStep(OrderProcess op, String rootOrderId) {
		// basically we get possible steps from process (filter out flow control elements) and register first step at machine
		List<CapabilityInvocation> stepCandidates = op.getAvailableSteps().stream()
				.filter(step ->(step instanceof CapabilityInvocation) )
				.map(CapabilityInvocation.class::cast)
				.filter(capInv -> capInv.getInvokedCapability() != null)
				.collect(Collectors.toList());
		if (stepCandidates.isEmpty()) {
			// check if process is finished, then we can remove it from the shopfloor 
			if (op.areAllTasksCancelledOrCompleted()) {
				// no need to signal completion here, as step level completion already happended, and completion only when process arrives at output station
				log.info(String.format("OrderProcess %s is complete, now triggering move to output station",rootOrderId));
				//TODO: --> transport it off to output station
			} else {
				// this implies that the process cant make progress as there is no available capabilityinvocation ie. process step to allocate to a machine
				log.warning(String.format("OrderProcess %s has no available steps of type CapabilityInvocation to continue with",rootOrderId));
				orderEventBus.tell(new OrderEvent(rootOrderId, this.self().path().name(), OrderEventType.CANCELED), ActorRef.noSender());		
				//TODO: if this process is somewhere on some machine, remove it from production --> transport it off to output station
			}
		} else {
			// go through each candidate and check if any of the steps can be mapped to a available machine,
			// first get candidates where the machine capabilites matches
			Map<CapabilityInvocation,Set<AkkaActorBackedCoreModelAbstractActor>> capMap = stepCandidates.stream()
				.map(step -> new AbstractMap.SimpleEntry<CapabilityInvocation,Set<AkkaActorBackedCoreModelAbstractActor>>(step, capMan.getMachinesProvidingCapability(step.getInvokedCapability())) )
				.filter(pair -> !pair.getValue().isEmpty() )
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (prev, next) -> next, HashMap::new));
				
			
			// select all candidate steps that have some actor allocation to modelActors 						
//			List<ActorAllocation> aaOpts = stepCandidates.stream()
//					.map(cap -> op.getActorAllocationForProcess(cap))
//					.filter(allocOpt -> allocOpt.isPresent())
//					.map(allocOpt -> allocOpt.get()) // here we have mapped steps,
//					.collect(Collectors.toList());
			if (capMap.isEmpty()) {
				log.warning(String.format("OrderProcess %s has no available CapabilityInvocations steps match a discovered Machine/Actor, Order is paused",rootOrderId));
				ordMapper.pauseOrder(rootOrderId);
			} else { // among those with available capabilities, select first where there is a machine mapped and available at the moment, implement JIT assignment
				
				// for each step produce tuple list of available machines
				Optional<AbstractMap.SimpleEntry<CapabilityInvocation, AkkaActorBackedCoreModelAbstractActor>> maOpt = capMap.entrySet().stream()
						.flatMap(pair -> pair.getValue().stream().map(mach -> new AbstractMap.SimpleEntry<>(pair.getKey(), mach)))
						.filter(flatPair -> flatPair.getValue() != null)
						.findFirst(); // then from this list pick the first
				
//						.map(alloc -> new AbstractMap.SimpleEntry<>(alloc, capMan.resolveByModelActor(alloc.getActor())) )
//						.filter(allocMap -> allocMap.getValue().isPresent())
//						.map(allocMap -> new AbstractMap.SimpleEntry<>(allocMap.getKey(), allocMap.getValue().get()))					
//						.filter(allocMap -> ordMapper.isMachineIdle(allocMap.getValue()) ) // we have an idle machine 
//						.findFirst();
				
				maOpt.ifPresent(aa -> {								
					// scheduled, not yet producing
					ordMapper.requestMachineForOrder(aa.getValue(), rootOrderId, aa.getKey());	
					// get machine actor reference				
					aa.getValue().getAkkaActor().tell( new RegisterProcessStepRequest(rootOrderId, aa.getKey().toString(), aa.getKey(), this.self()), this.self());					
					//TODO: alternatively the machine could issue a SCHEDULED event
					ordMapper.scheduleProcess(rootOrderId);
				});
				if (!maOpt.isPresent()) {
					log.info(String.format("OrderProcess %s has no available CapabilityInvocations steps that match an available Machine/Actor, Order is paused",rootOrderId));
					ordMapper.pauseOrder(rootOrderId);
				}
			}
		}
	}

	private void produceProcessAtMachine(ReadyForProcessEvent readyE) {
		String orderId = readyE.getResponseTo().getRootOrderId();
		capMan.resolveByAkkaActor(getSender()).ifPresent(machine -> {
			if (readyE.isReady()) {		
				// TODO: theoretically we would need to check if we canceled the order in the meantime, then we wont allocated that order but rather free up the machine
				ordMapper.reserveOrderAtMachine(machine);			
				this.getSender().tell(new LockForOrder(readyE.getResponseTo().getProcessStepId(), orderId), this.getSelf());
				//upon lockfororder,the machine should transition into starting state
				//TODO this should actually be published by the machine
				ordMapper.allocateProcess(orderId);
				// request transport to that machine: this machine should now be in Starting state
				requestTransport(machine, orderId);	
				// update that we now work on an order
				ordMapper.getOrderRequest(orderId).ifPresent(rpr -> {
					ProcessChangeImpact pci = rpr.getProcess().activateStepIfAllowed(readyE.getResponseTo().getProcessStep());
					orderEventBus.tell( new OrderProcessUpdateEvent(orderId, this.self().path().name(), pci), ActorRef.noSender() );
				});
				
			} else { // e.g., when machine needs to go down for maintenance, or some other error occured in the meantime						
				ordMapper.freeUpMachine(machine);			
				ordMapper.pauseOrder(readyE.getResponseTo().getRootOrderId());
				// then Wait for event on status update
			}
		});
	}
	
	private void handleNoLongerAvailableMachine(MachineDisconnectedEvent mde) {		
		capMan.removeActor(mde.getMachine());
		Optional<MachineOrderMappingStatus> momStatus = ordMapper.removeMachine(mde.getMachine());
		momStatus.ifPresent(mom -> {
			// check if it was producing anything
			if (mom.getOrderId() == null) return;			
			// TODO: if so, where that product/pallet/order currently is
			// check if it was allocated to produce something
			// if so, undo order allocation	
		});
		
	}
	
	private void handleNewlyAvailableMachine(MachineConnectedEvent mce) {
		capMan.setCapabilities(mce);
		log.info("Storing Capabilities for machine: "+mce.getMachineId());
		// now wait for machine available event to make use of it (currently we dont know its state)				
	}
	
	private void handleProductionCompletionEvent() {
		// just complete the processstep in the process if not already done so
		
	}
	
	private void handleMachineUpdateEvent(MachineUpdateEvent mue) {
		log.info(String.format("MachineUpdateEvent for machine %s %s : %s", mue.getMachineId(), mue.getParameterName(), mue.getNewValue().toString()));
		capMan.resolveById(mue.getMachineId()).ifPresent(machine -> {
			// will only process event if the parameter changes is "STATE"
			ordMapper.updateMachineStatus(machine, mue);
			if (mue.getParameterName().equals(MachineOrderMappingManager.STATE_VAR_NAME)) {
				if (mue.getNewValue().equals(MachineOrderMappingManager.IDLE_STATE_VALUE)) {
					// if idle --> machine ready --> lets check if any order is waiting for that machine
					ordMapper.getPausedProcessesOnSomeMachine().stream()
						.forEach(rpr -> tryAssignExecutingMachineForOneProcessStep(rpr.getProcess(), rpr.getRootOrderId()));
					// we don't abort upon first success but try for every one, should matter, just takes a bit of processing
					//if none, then just wait for next incoming event
				} else if (mue.getNewValue().equals(MachineOrderMappingManager.COMPLETING_STATE_VALUE)) {
					// TODO: step done, now update the process, so which step has been completed?
					// we cannot rely on ProductionCompletionevent as there could be a race condition which event arrives first
					
					// then we still have the order occupying the machine  --> now check if next place is ready somewhere
					// update process, get next step --> check if possible somewhere
					ordMapper.getOrderRequestOnMachine(machine).ifPresent(rpr -> { 
						 ordMapper.getJobOnMachine(machine).ifPresent(step -> { 
							 ProcessChangeImpact pci = rpr.getProcess().markStepComplete(step); 
							 orderEventBus.tell( new OrderProcessUpdateEvent(rpr.getRootOrderId(), this.self().path().name(), pci), ActorRef.noSender() );
							 } );
						tryAssignExecutingMachineForOneProcessStep(rpr.getProcess(), rpr.getRootOrderId()); });
				} else if (mue.getNewValue().equals(MachineOrderMappingManager.PRODUCING_STATE_VALUE)) {
					// now update mapping that order has arrived at that machine and is loaded
					ordMapper.confirmOrderAtMachine(machine);
				}
			}						
		});		
		
		
	}
	
	private void requestTransport(AkkaActorBackedCoreModelAbstractActor destination, String orderId) {
		
		Optional<AkkaActorBackedCoreModelAbstractActor> currentLoc = ordMapper.getCurrentMachineOfOrder(orderId);
		if (!currentLoc.isPresent()) {
			log.warning("Unable to located current machine of order: "+orderId);
		} else {
			
		}
		
	}
	
	private void handleResponseToTransportRequest() {
		
	}
	

	// manage a list of loading and unloading stations (at least one each needs to be available)
	// when loading/input station is loaded --> then assign to order
	// for unloading/output station: just track if it is empty

	
	
}
>>>>>>> Stashed changes
