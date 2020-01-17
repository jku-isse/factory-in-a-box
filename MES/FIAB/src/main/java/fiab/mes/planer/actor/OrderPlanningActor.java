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
import fiab.mes.machine.actor.WellknownMachinePropertyFields;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.machine.msg.MachineDisconnectedEvent;
import fiab.mes.machine.msg.MachineEvent.MachineEventType;
import fiab.mes.machine.msg.MachineStatus;
import fiab.mes.machine.msg.MachineStatusUpdateEvent;
import fiab.mes.machine.msg.MachineUpdateEvent;
import fiab.mes.order.MappedOrderProcess;
import fiab.mes.order.OrderProcess;
import fiab.mes.order.OrderProcess.ProcessChangeImpact;
import fiab.mes.order.msg.CancelOrTerminateOrder;
import fiab.mes.order.msg.LockForOrder;
import fiab.mes.order.msg.OrderEvent;
import fiab.mes.order.msg.OrderEvent.OrderEventType;
import fiab.mes.order.msg.OrderProcessUpdateEvent;
import fiab.mes.order.msg.ReadyForProcessEvent;
import fiab.mes.order.msg.RegisterProcessRequest;
import fiab.mes.order.msg.RegisterProcessStepRequest;
import fiab.mes.planer.actor.MachineOrderMappingManager.MachineOrderMappingStatus;
import fiab.mes.planer.actor.MachineOrderMappingManager.MachineOrderMappingStatus.AssignmentState;
import fiab.mes.planer.actor.MachineOrderMappingManager.MachineOrderMappingStatusLifecycleException;
import fiab.mes.planer.msg.PlanerStatusMessage;
import fiab.mes.planer.msg.PlanerStatusMessage.PlannerState;
import fiab.mes.transport.actor.transportsystem.TransportSystemCoordinatorActor;
import fiab.mes.transport.handshake.HandshakeProtocol;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerSide;
import fiab.mes.transport.msg.RegisterTransportRequest;
import fiab.mes.transport.msg.RegisterTransportRequestStatusResponse;


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
	protected ActorSelection transportCoordinator;
	protected ActorRef self;
	protected PlannerState state = PlannerState.STOPPED;
	
	static public Props props() {	    
		return Props.create(OrderPlanningActor.class, () -> new OrderPlanningActor());
	}

	public OrderPlanningActor() {
		this.self = getSelf();
		try {
			getEventBusAndSubscribe();
			getTransportSystemCoordinator();
		} catch (Exception e) {
			state = PlannerState.FAILED_START;
			this.context().parent().tell(new PlanerStatusMessage(WELLKNOWN_LOOKUP_NAME, MachineEventType.INITIALIZING, state, "Failed to start due to: "+e.getMessage()), this.self());
		}
		state = PlannerState.DEGRADED_MODE;
		publishLocalState(MachineEventType.UPDATED, state, "Waiting for shopfloor input and output stations.");
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(RegisterProcessRequest.class, rpReq -> {
					log.info("Received Register Order Request: "+rpReq.getRootOrderId());		        	
					//reqIndex.put(rpReq.getRootOrderId(), rpReq);
					if (state.equals(PlannerState.FULLY_OPERATIONAL)) {
						ordMapper.registerOrder(rpReq);
						scheduleProcess(rpReq.getRootOrderId(), rpReq.getProcess());
					} else {
						String msg = String.format("Cannot accept request for process %s when not in %s state, currently in state %s",rpReq.getRootOrderId(), PlannerState.FULLY_OPERATIONAL, state);
						log.warning(msg);
						orderEventBus.tell(new OrderEvent(rpReq.getRootOrderId(), this.self().path().name(), OrderEventType.REJECTED, msg), ActorRef.noSender());
					}
				}) 
				.match(ReadyForProcessEvent.class, readyE -> {
					produceProcessAtMachine(readyE);
				})
				.match(OrderEvent.class, orderEvent -> {					
					//TODO: to confirm that an order is now produced or completed at a machine
				})
				.match(MachineConnectedEvent.class, machineEvent -> {
					handleNewlyAvailableMachine(machineEvent);
				})
				.match(MachineDisconnectedEvent.class, machineEvent -> {
					handleNoLongerAvailableMachine(machineEvent);
				})
				.match(MachineStatusUpdateEvent.class, machineEvent -> {
					handleMachineUpdateEvent(machineEvent);
				})
				.match(IOStationStatusUpdateEvent.class, ioStationEvent -> {
					handleIOStationUpdateEvent(ioStationEvent);
				})
				.match(RegisterTransportRequestStatusResponse.class, resp -> {
					handleResponseToTransportRequest(resp);
				})
				.build();
	}

	private void getEventBusAndSubscribe() throws Exception{
		SubscribeMessage orderSub = new SubscribeMessage(getSelf(), new SubscriptionClassifier(WELLKNOWN_LOOKUP_NAME, "*"));		
		orderEventBus = this.context().actorSelection("/user/"+OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		orderEventBus.tell(orderSub, getSelf());
		
		SubscribeMessage machineSub = new SubscribeMessage(getSelf(), new SubscriptionClassifier(WELLKNOWN_LOOKUP_NAME, "*"));
		machineEventBus = this.context().actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		machineEventBus.tell(machineSub, getSelf());
		
		ordMapper = new MachineOrderMappingManager(orderEventBus, self().path().name());
	}
	
	private void getTransportSystemCoordinator() throws Exception {
		transportCoordinator = this.context().actorSelection("/user/"+TransportSystemCoordinatorActor.WELLKNOWN_LOOKUP_NAME);
	}
	
	private void publishLocalState(MachineEventType eventType, PlannerState state, String message) {
		machineEventBus.tell(new PlanerStatusMessage(WELLKNOWN_LOOKUP_NAME, eventType, state, message), this.self());
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
			String msg = String.format("OrderProcess %s does not have all leaf nodes with capability invocations, thus cannot be completely mapped to machines, cancelling order", rootOrderId);
			log.warning(msg);
			ordMapper.removeOrder(rootOrderId); // we didn/t start processing yet, so we can just drop the process
			orderEventBus.tell(new OrderEvent(rootOrderId, this.self().path().name(), OrderEventType.REJECTED, msg), ActorRef.noSender());
		} else {
			// we have capability invocations with capabilities, thus able to map them to machines
			ProcessChangeImpact pci = mop.activateProcess();
			String msg = "Capability invocations have capabilities, thus able to map them to machines. rootOrderId: "+rootOrderId;
			orderEventBus.tell( new OrderProcessUpdateEvent(rootOrderId, this.self().path().name(), msg, pci), ActorRef.noSender() );
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
				//transport it off to output station
				ordMapper.markOrderCompleted(rootOrderId, "Order about to be moved to next available/idle output station");
				Optional<AkkaActorBackedCoreModelAbstractActor> outputStation = tryReserveOutputStation(rootOrderId);
				if (!outputStation.isPresent()) {
				// if not possible we wont do anything with this order at the moment and wait for outputstation to become available
					return;
				} else {// else, we have now  the reservation of the outputstation for this order and we continue with transport
					requestTransport(outputStation.get(), rootOrderId);
				}
			} else {
				// this implies that the process cant make progress as there is no available capabilityinvocation ie. process step to allocate to a machine
				String msg = String.format("OrderProcess %s has no available steps of type CapabilityInvocation to continue with, moving to next available/idle output station",rootOrderId); 
				log.warning(msg);
				ordMapper.markOrderCanceled(rootOrderId, msg);
				//if this process is somewhere on some machine, remove it from production --> transport it off to output station
				Optional<AkkaActorBackedCoreModelAbstractActor> outputStation = tryReserveOutputStation(rootOrderId);
				if (!outputStation.isPresent()) {
				// if not possible we wont do anything with this order at the moment and wait for outputstation to become available
					return;
				} else {// else, we have now  the reservation of the outputstation for this order and we continue with transport
					requestTransport(outputStation.get(), rootOrderId);
				}
			}
		} else {
			// go through each candidate and check if any of the steps can be mapped to a available machine,
			// first get candidates where the machine capabilites matches
			Map<CapabilityInvocation,Set<AkkaActorBackedCoreModelAbstractActor>> capMap = stepCandidates.stream()
				.map(step -> new AbstractMap.SimpleEntry<CapabilityInvocation,Set<AkkaActorBackedCoreModelAbstractActor>>(step, capMan.getMachinesProvidingCapability(step.getInvokedCapability())) )
				.filter(pair -> !pair.getValue().isEmpty() )
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (prev, next) -> next, HashMap::new));
				
			if (capMap.isEmpty()) {
				log.warning(String.format("OrderProcess %s has no available CapabilityInvocations steps match a discovered Machine/Actor, Order is paused",rootOrderId));
				ordMapper.pauseOrder(rootOrderId);
			} else { // among those with available capabilities, select first where there is a machine mapped and available at the moment, implement JIT assignment
				
				// for each step produce tuple list of available machines that match the capability required
				Optional<AbstractMap.SimpleEntry<CapabilityInvocation, AkkaActorBackedCoreModelAbstractActor>> maOpt = capMap.entrySet().stream()
						.flatMap(pair -> pair.getValue().stream().map(mach -> new AbstractMap.SimpleEntry<>(pair.getKey(), mach)))
						.filter(flatPair -> {
							return flatPair.getValue() != null;
						})
						.filter(pair -> {
							return ordMapper.isMachineIdle(pair.getValue());
						}) //continue with those where actor is idle
						.filter(pair -> {
							return ordMapper.getMappingStatusOfMachine(pair.getValue()).orElse(new MachineOrderMappingStatus(pair.getValue(), AssignmentState.UNKNOWN)).getAssignmentState().equals(AssignmentState.NONE); // and also where we havent assigned any order to yet
						})
						.findFirst(); // then from this list pick the first
				
				maOpt.ifPresent(aa -> {								
					// scheduled, not yet producing
					Optional<AkkaActorBackedCoreModelAbstractActor> currentLoc = ordMapper.getCurrentMachineOfOrder(rootOrderId);
					if (!currentLoc.isPresent()) {												
//						assign inputstation 
						Optional<AkkaActorBackedCoreModelAbstractActor> inputStation = tryReserveNewProcessAtInputStation(rootOrderId);
						if (!inputStation.isPresent()) {
						// if not possible we wont do anything with this order at the moment and wait for inputstation to become available
							log.info(String.format("Order %s and potential (not yet requested) machine %s waiting for next available inputstation", rootOrderId, aa.getValue().getId()));
							return;
						} 
					} 
					// order at machine, we now register/request that machine  
					try {
						ordMapper.requestMachineForOrder(aa.getValue(), rootOrderId, aa.getKey());
					} catch (MachineOrderMappingStatusLifecycleException e) {
						// TODO Auto-generated catch block
						// should not occur
						e.printStackTrace();						
					}					
					//TODO: alternatively the machine could issue a SCHEDULED event (BUT WE NEED TO TRACK OUR REQUESTS)
					ordMapper.scheduleProcess(rootOrderId);
					// get machine actor reference				
					log.info(String.format("Order %s about to be registered at machine %s", rootOrderId, aa.getValue().getId()));
					aa.getValue().getAkkaActor().tell( new RegisterProcessStepRequest(rootOrderId, aa.getKey().toString(), aa.getKey(), this.self()), this.self());					

				});
				if (!maOpt.isPresent()) {
					log.info(String.format("OrderProcess %s has no available CapabilityInvocations steps that match an available Machine/Actor, Order is paused",rootOrderId));
					ordMapper.pauseOrder(rootOrderId);
				}
			}
		}
	}
	
	private Optional<AkkaActorBackedCoreModelAbstractActor> tryReserveNewProcessAtInputStation(String orderId) {
		Set<AkkaActorBackedCoreModelAbstractActor> iStations = capMan.getMachinesProvidingCapability(inputStationCap);
		// get one empty inputstation
		Optional<AkkaActorBackedCoreModelAbstractActor> availInput = iStations.stream()
			.filter(io -> ordMapper.getMappingStatusOfMachine(io).orElse(new MachineOrderMappingStatus(io, AssignmentState.UNKNOWN)).getAssignmentState().equals(AssignmentState.NONE))
			.findAny();
		return availInput.map(iStation -> {
			try {
				ordMapper.requestMachineForOrder(iStation, orderId, null); // null JobId as we request transport
				ordMapper.confirmOrderAtMachine(iStation); //and we occupy this machine immediately, so we can "locate" it when requesting transport
			} catch (MachineOrderMappingStatusLifecycleException e) {
				e.printStackTrace();
				return null;
			} 
			return iStation;
		});
	}
	
	private Optional<AkkaActorBackedCoreModelAbstractActor> tryReserveOutputStation(String orderId) {
		Set<AkkaActorBackedCoreModelAbstractActor> oStations = capMan.getMachinesProvidingCapability(outputStationCap);
		// get one empty inputstation
		Optional<AkkaActorBackedCoreModelAbstractActor> availOutput = oStations.stream()
			.filter(io -> ordMapper.getMappingStatusOfMachine(io).orElse(new MachineOrderMappingStatus(io, AssignmentState.UNKNOWN)).getAssignmentState().equals(AssignmentState.NONE))
			.findAny();
		return availOutput.map(oStation -> {
			try {
				ordMapper.requestMachineForOrder(oStation, orderId, null); // null JobId as we request transport
				ordMapper.reserveOrderAtMachine(oStation);
			} catch (MachineOrderMappingStatusLifecycleException e) {
				e.printStackTrace();
				return null;
			} 
			return oStation;
		});
	}

	private void produceProcessAtMachine(ReadyForProcessEvent readyE) {
		String orderId = readyE.getResponseTo().getRootOrderId();
		capMan.resolveByAkkaActor(getSender()).ifPresent(machine -> {
			if (readyE.isReady()) {		
				ordMapper.reserveOrderAtMachine(machine);			
				this.getSender().tell(new LockForOrder(readyE.getResponseTo().getProcessStepId(), orderId), this.getSelf());
				//upon lockfororder,the machine should transition into starting state
				//FIXME this should actually be published by the machine
				ordMapper.allocateProcess(orderId);
				// request transport to that machine: this machine should now be in Starting state
				requestTransport(machine, orderId); // if not transport is ready, here we stay in allocated state	
				
				//FIXME: this should be triggered by the machine event, not here as we haven't transported anything yet
				// update that we now work on an order
				ordMapper.getOrderRequest(orderId).ifPresent(rpr -> {
					ProcessChangeImpact pci = rpr.getProcess().activateStepIfAllowed(readyE.getResponseTo().getProcessStep());
					String msg = String.format("Machine with ID %s agreed to work on Order with ID %s", machine.getId(), orderId);
					log.info(msg);
					orderEventBus.tell( new OrderProcessUpdateEvent(orderId, this.self().path().name(), msg, pci), ActorRef.noSender() );
				});
				
			} else { // e.g., when machine needs to go down for maintenance, or some other error occured in the meantime						
				ordMapper.freeUpMachine(machine, true);			
				ordMapper.pauseOrder(readyE.getResponseTo().getRootOrderId());
				// then Wait for event on status update
				//check if inputstation was reserved, free up as well:
				ordMapper.getCurrentMachineOfOrder(orderId).ifPresent( somewhere -> {
					capMan.getMachinesProvidingCapability(inputStationCap).stream()
						.filter(iStation -> iStation.equals(somewhere)) // found order at istation
						.findAny().ifPresent(iStation -> {
							// deallocate and check for others that might wanna make use of it
							ordMapper.freeUpMachine(iStation, false);
							ordMapper.getProcessesInState(OrderEventType.PAUSED).stream()
							.forEach(rpr -> tryAssignExecutingMachineForOneProcessStep(rpr.getProcess(), rpr.getRootOrderId()));
						});
				});
			}
		});
	}
		
	private void handleIOStationUpdateEvent(IOStationStatusUpdateEvent ue) {
		log.info(String.format("IOStationUpdateEvent for machine %s : %s", ue.getMachineId(), ue.getStatus().toString()));
		capMan.resolveById(ue.getMachineId()).ifPresent(machine -> {
			ordMapper.updateMachineStatus(machine, ue);
			if (ue.getStatus().equals(ServerSide.IdleEmpty)) {
				// we reached idle for an outputstation
				ordMapper.getProcessesInState(OrderEventType.COMPLETED).stream()
				.forEach(rpr -> tryAssignExecutingMachineForOneProcessStep(rpr.getProcess(), rpr.getRootOrderId()));
				ordMapper.getProcessesInState(OrderEventType.CANCELED).stream() //orders that need to be prematurely removed
				.forEach(rpr -> tryAssignExecutingMachineForOneProcessStep(rpr.getProcess(), rpr.getRootOrderId()));
			} else if (ue.getStatus().equals(ServerSide.IdleLoaded)) {
				// we reached idle for an inputstation
				// we are ready to start a new process as this input is ready
				ordMapper.getProcessesInState(OrderEventType.PAUSED).stream()
					.forEach(rpr -> tryAssignExecutingMachineForOneProcessStep(rpr.getProcess(), rpr.getRootOrderId()));
			}
		});
	}

	
	private void handleMachineUpdateEvent(MachineStatusUpdateEvent mue) {
		log.info(String.format("MachineUpdateEvent for machine %s : %s", mue.getMachineId(), mue.getStatus().toString()));
		capMan.resolveById(mue.getMachineId()).ifPresent(machine -> {
			// will only process event if the parameter changes is "STATE"
			ordMapper.updateMachineStatus(machine, mue);
			if (mue.getParameterName().equals(WellknownMachinePropertyFields.STATE_VAR_NAME)) {

				if (mue.getStatus().equals(MachineStatus.IDLE)) {
					// if idle --> machine ready --> lets check if any order is waiting for that machine
					ordMapper.getPausedProcessesOnSomeMachine().stream()
						.forEach(rpr -> tryAssignExecutingMachineForOneProcessStep(rpr.getProcess(), rpr.getRootOrderId()));
					// we don't abort upon first success but try for every one, shouldn't matter, just takes a bit of processing
					
					// order that are paused but not assigned to any machine yet (not efficient as will check all those already checked above)
					ordMapper.getProcessesInState(OrderEventType.PAUSED).stream()
						.forEach(rpr -> tryAssignExecutingMachineForOneProcessStep(rpr.getProcess(), rpr.getRootOrderId()));
					
					//if none, then just wait for next incoming event
				} else if (mue.getStatus().equals(MachineStatus.COMPLETING)) {
					// TODO: step done, now update the process, so which step has been completed?
					// we cannot rely on ProductionCompletionevent as there could be a race condition which event arrives first
					
					// then we still have the order occupying the machine  --> now check if next place is ready somewhere
					// update process, get next step --> check if possible somewhere
					ordMapper.getOrderRequestOnMachine(machine).ifPresent(rpr -> { 
						 log.debug(String.format("%s in state %s with register process request of %s", machine.getId(), mue.getStatus().toString(), rpr.getRootOrderId()));
						 ordMapper.getJobOnMachine(machine).ifPresent(step -> { 
							 ProcessChangeImpact pci = rpr.getProcess().markStepComplete(step); 
							 String msg = String.format("Update process that step %s is complete at machine with ID %s and with order ID %s", step.getDisplayName(), machine.getId(), rpr.getRootOrderId());
							 log.info(msg);
							 OrderProcessUpdateEvent opue = new OrderProcessUpdateEvent(rpr.getRootOrderId(), this.self().path().name(), msg, pci);
							 orderEventBus.tell(opue, ActorRef.noSender());
							 } );
						tryAssignExecutingMachineForOneProcessStep(rpr.getProcess(), rpr.getRootOrderId()); });
				} else if (mue.getStatus().equals(MachineStatus.EXECUTE)) {
					// now update mapping that order has arrived at that machine and is loaded
					ordMapper.confirmOrderAtMachine(machine);
				}
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
		checkIOStations();
	}
	
	private void handleNewlyAvailableMachine(MachineConnectedEvent mce) {
		capMan.setCapabilities(mce);
		log.info("Storing Capabilities for machine: "+mce.getMachineId());
		//check for input/output stations
		checkIOStations();
		// now wait for machine available event to make use of it (currently we dont know its state)	
	}
	
	protected AbstractCapability inputStationCap = HandshakeProtocol.getInputStationCapability();
	protected AbstractCapability outputStationCap = HandshakeProtocol.getOutputStationCapability();
	
	private void checkIOStations() {
		PlannerState currState = state;
		PlannerState newState = PlannerState.STOPPED;
		if (capMan.getMachinesProvidingCapability(inputStationCap).size() > 0 && 
				capMan.getMachinesProvidingCapability(outputStationCap).size() > 0) {
			newState = PlannerState.FULLY_OPERATIONAL;
		} else {
			newState = PlannerState.DEGRADED_MODE;
		}
		if (newState != currState) {
			state = newState;
			publishLocalState(MachineEventType.UPDATED, state, "Input/Outputstation availability changed");
		}
	}
	
	private void handleProductionCompletionEvent() {
		// just complete the processstep in the process if not already done so
		
	}
	

	private void requestTransport(AkkaActorBackedCoreModelAbstractActor destination, String orderId) {
		
		Optional<AkkaActorBackedCoreModelAbstractActor> currentLoc = ordMapper.getCurrentMachineOfOrder(orderId);
		if (!currentLoc.isPresent()) {
			String msg = "Unable to located current machine of order: "+orderId;
			log.warning(msg);
			// now this is really an error as we ensure when requesting a machine that we do this only when first reserving and allocating an inputstation
			ordMapper.markOrderFailedTransport(orderId, msg);
			// free up destination
			ordMapper.freeUpMachine(destination, true);
			//TODO: if outputstation then dont need a new event, depending where the request failed, perhaps already at new machine?!
			// TODO: if source is inputstation, then freeup also input station from reservation
		} else {
			ordMapper.markOrderWaitingForTransport(orderId);
			transportCoordinator.tell(new RegisterTransportRequest(currentLoc.get(), destination, orderId, self), self);
		}
		
	}
	
	private void handleResponseToTransportRequest(RegisterTransportRequestStatusResponse resp) {
		String orderId = resp.getOriginalRequest().getOrderId();
		String msg = "";
		switch(resp.getResponse()) {
		case COMPLETED:
			log.info(String.format("Transport of Order %s complete", orderId));
			ordMapper.markOrderCompletedTransport(orderId); 
			AkkaActorBackedCoreModelAbstractActor machine2 = resp.getOriginalRequest().getDestination();
			ordMapper.confirmOrderAtMachine(machine2);
			//all is fine, nothing to do, machine takes over and produces
			// or outputstation has received, lets check this
			capMan.getMachinesProvidingCapability(outputStationCap).stream()
				.filter(machine -> machine.equals(machine2))
				.findAny()
				.ifPresent(oStation -> { // an output station
					ordMapper.markOrderRemovedFromShopfloor(orderId, "Order left the shopfloor via Outputmachine: "+oStation.getId());
				});
			// freeup allocation of source machine
			ordMapper.removeOrderAllocationIfMachineStillOccupied(resp.getOriginalRequest().getSource());
			break;
		case FAILED_IN_TRANSPORT:
			msg = String.format("Transport of Order %s failed with reason %s : %s", orderId, resp.getResponse(), resp.getMessage());
			ordMapper.markOrderFailedTransport(orderId, msg);
			log.warning(msg);
			// we need to manually remove the pallet and reset the transport modules, nothing we can handle here.
			ordMapper.freeUpMachine(resp.getOriginalRequest().getDestination(), true);
			//TODO: if outputstation then dont need a new event, depending where the request failed, perhaps already at new machine?!
			// TODO: if source is inputstation, then freeup also input station from reservation
			AkkaActorBackedCoreModelAbstractActor machine1 = resp.getOriginalRequest().getDestination();
			String jobId1 = ordMapper.getJobOnMachine(machine1).map( step -> step.getID()).orElse("");
			machine1.getAkkaActor().tell(new CancelOrTerminateOrder(resp.getOriginalRequest().getOrderId(), jobId1), self);
			break;
		case MISSING_TRANSPORT_MODULE: //fallthrough
		case NO_ROUTE: //fallthrough
		case UNSUPPORTED_ENDPOINT_POSITIONS: //fallthrough
		case UNSUPPORTED_TRANSIT_POSITION: //fallthrough
			msg = String.format("Transport of Order %s failed with reason %s : %s", orderId, resp.getResponse(), resp.getMessage());
			ordMapper.markOrderDeniedTransport(orderId, msg);
			log.warning(msg);
			// we need to manually remove the pallet from machine and reset the machine, nothing we can handle here.
			//deallocate destination machine from reservation
			AkkaActorBackedCoreModelAbstractActor machine = resp.getOriginalRequest().getDestination();
			String jobId = ordMapper.getJobOnMachine(machine).map( step -> step.getID()).orElse("");
			ordMapper.freeUpMachine(resp.getOriginalRequest().getDestination(), true);
			//TODO: if destination is outputstation then dont need a new event,
			// TODO: if source is inputstation, then freeup also input station from reservation 
			// tell machine to reset, resp drop current reservation
			machine.getAkkaActor().tell(new CancelOrTerminateOrder(resp.getOriginalRequest().getOrderId(), jobId), self);
			break;
		case QUEUED:
			// all is fine, transport is just not ready yet
			log.info(String.format("Transport of Order %s accepted but not ready yet", orderId));
			break;	
		case ISSUED:
			// all is fine, transport now is going to start
			ordMapper.markOrderInTransit(orderId);
			log.info(String.format("Transport of Order %s about to start", orderId));
			break;
		default:
			break;
		}
	}
	

	// manage a list of loading and unloading stations (at least one each needs to be available)
	// when loading/input station is loaded --> then assign to order
	// for unloading/output station: just track if it is empty

	
	
	
}

