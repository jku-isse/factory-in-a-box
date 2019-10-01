package fiab.mes.planer.actor;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ProcessCore.ProcessStep;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.msg.MachineUpdateEvent;
import fiab.mes.order.msg.OrderEvent.OrderEventType;
import fiab.mes.planer.actor.MachineOrderMappingManager.MachineOrderMappingStatus.AssignmentState;
import fiab.mes.order.OrderProcess;
import fiab.mes.order.msg.OrderEvent;
import fiab.mes.order.msg.RegisterProcessRequest;

public class MachineOrderMappingManager {

	private static final Logger logger = LoggerFactory.getLogger(MachineOrderMappingManager.class);
	
	public static final String IDLE_STATE_VALUE = "IDLE";
	public static final String COMPLETING_STATE_VALUE = "STOPPING";
	public static final String PRODUCING_STATE_VALUE = "EXECUTE";
	public static final String STATE_VAR_NAME = "STATE";
	
	protected ActorSelection orderEventBus; 
	protected String publisherName;
	protected Map<AkkaActorBackedCoreModelAbstractActor,MachineOrderMappingStatus> moms = new HashMap<>();
	protected Map<String,SimpleEntry<RegisterProcessRequest, OrderEventType>> orders = new HashMap<>();
	
	public MachineOrderMappingManager(ActorSelection orderEventBus, String parentActorName) {
		this.orderEventBus = orderEventBus;
		this.publisherName = parentActorName;
	}
	
	private void publishEvent(String orderId, OrderEventType type) {
		orderEventBus.tell(new OrderEvent(orderId, publisherName, type), ActorRef.noSender());
	}
	
	public Optional<RegisterProcessRequest> getOrderRequest(String orderId) {
		if (orders.containsKey(orderId)) {
			return Optional.of(orders.get(orderId).getKey());
		} else return Optional.empty();
	}
	
	public boolean isOrderMappedAtLeastOneMachine(String orderId) {
		return moms.values().stream()
		.filter(mom -> mom.getOrderId().equals(orderId))
		.findAny()
		.isPresent();
	}
	
	public void registerOrder(RegisterProcessRequest rpReq) {
		orders.put(rpReq.getRootOrderId(), new SimpleEntry<RegisterProcessRequest, OrderEventType>(rpReq, OrderEventType.REGISTERED));
		publishEvent(rpReq.getRootOrderId(), OrderEventType.REGISTERED);
	}
	
	public void pauseOrder(String orderId) {
		transitionOrder(orderId, OrderEventType.PAUSED);
	}
	
	private void transitionOrder(String orderId, OrderEventType newState) {
		Optional.of(orders.get(orderId)).ifPresent(pair -> {
			if (pair.getValue() != newState) { // only when not paused yet
				pair.setValue(newState);
				publishEvent(orderId, newState);
			}
		});
	}
	
	public void allocateProcess(String orderId) {
		transitionOrder(orderId, OrderEventType.ALLOCATED);
	}
	
	public void scheduleProcess(String orderId) {
		transitionOrder(orderId, OrderEventType.SCHEDULED);
	}
	
	public void removeOrder(String orderId) {
		orders.remove(orderId);
	}
	
	public List<RegisterProcessRequest> getProcessesInState(OrderEventType state) {
		return orders.values().stream()
			.filter(valuePair -> valuePair.getValue().equals(state))
			.map(valuePair -> valuePair.getKey())
			.collect(Collectors.toList());
	}
	
	// find any process that is currently paused (waiting for next available machine) and currently located on a machine (thus blocking it)
	public List<RegisterProcessRequest> getPausedProcessesOnSomeMachine() {
		// find occupying processes, 
		Set<String> occupProcesses = moms.values().stream()
	 	.filter(mapping -> mapping.getAllocationState().equals(AssignmentState.OCCUPIED))
	 	.map(mapping -> mapping.getOrderId())
	 	.collect(Collectors.toSet());
		// and intersect with those paused
		return getProcessesInState(OrderEventType.PAUSED).stream()
		  .filter(req -> occupProcesses.contains(req.getRootOrderId()))
		  .collect(Collectors.toList());
	}
	
	public Optional<RegisterProcessRequest> getOrderRequestOnMachine(AkkaActorBackedCoreModelAbstractActor machine) {
		Optional<RegisterProcessRequest> opReq = Optional.of(moms.get(machine))
				.map(mom -> getOrderRequest(mom.getOrderId()).orElse(null));
		return opReq;
	}
	
	public Optional<ProcessStep> getJobOnMachine(AkkaActorBackedCoreModelAbstractActor machine) {
		return Optional.of(moms.get(machine))
				.map(mom -> mom.getProductionJob());
	}
	
	public Set<String> getMappedProcessIds() {
		return moms.values().stream().map(mom -> mom.orderId).collect(Collectors.toSet());
	}
	
	public Optional<AkkaActorBackedCoreModelAbstractActor> getCurrentMachineOfOrder(String orderId) {		
		return moms.values().stream()
				.filter(mom -> mom.getOrderId().contentEquals(orderId))
				.filter(mom -> mom.getAllocationState().equals(AssignmentState.OCCUPIED) )
				.map(mom -> mom.getMachine())
				.findFirst();		
	}
	
	public boolean isMachineIdle(AkkaActorBackedCoreModelAbstractActor machine) {
		return getIdleMachines().stream().filter(idleM -> idleM.equals(machine)).findFirst().isPresent();
	}

	public List<AkkaActorBackedCoreModelAbstractActor> getIdleMachines() {
		return getMachinesInState(IDLE_STATE_VALUE);
	}
	
	public List<AkkaActorBackedCoreModelAbstractActor> getMachinesReadyForUnloading() {
		return getMachinesInState(COMPLETING_STATE_VALUE);
	}
	
	private List<AkkaActorBackedCoreModelAbstractActor> getMachinesInState(String state) {
		return moms.values().stream()
				.filter(mapping -> mapping.getLastMachineState().getNewValue().equals(state))
				.map(mapping -> mapping.getMachine())
				.collect(Collectors.toList());
	} 
	
	public void freeUpMachine(AkkaActorBackedCoreModelAbstractActor machine) {
		Optional.of(moms.get(machine)).ifPresent(mom -> { 
			mom.setOrderId(null); 
			mom.setAllocationState(AssignmentState.NONE); 
			}); 		
	}
	
	public void requestMachineForOrder(AkkaActorBackedCoreModelAbstractActor machine, String orderId, ProcessStep productionJob) {
		AssignmentState type = AssignmentState.REQUESTED;
		Optional.of(moms.get(machine)).ifPresent(mom -> { 
			mom.setAllocationState(type);
			mom.setOrderId(orderId);
			mom.setProductionJob(productionJob);
			logger.debug(String.format("Updateing OrderMapping for order %s from %s to %s", mom.getOrderId(), mom.getAllocationState(), type));			
		});					
	}
	
	public void reserveOrderAtMachine(AkkaActorBackedCoreModelAbstractActor machine) {
		updateMachineMappingStatus(machine, AssignmentState.RESERVED);						
	}
	
	public void confirmOrderAtMachine(AkkaActorBackedCoreModelAbstractActor machine) {
		updateMachineMappingStatus(machine, AssignmentState.OCCUPIED);					
	}
	
	private void updateMachineMappingStatus(AkkaActorBackedCoreModelAbstractActor machine, AssignmentState type) {
		Optional.of(moms.get(machine)).ifPresent(mom -> { 
			logger.debug(String.format("Updateing OrderMapping for order %s from %s to %s", mom.getOrderId(), mom.getAllocationState(), type));			
			mom.setAllocationState(type);
		});
	}
	
	public void updateMachineStatus(AkkaActorBackedCoreModelAbstractActor machine, MachineUpdateEvent event) {				
			moms.computeIfAbsent(machine, k -> new MachineOrderMappingStatus(machine, AssignmentState.NONE))
										.setLastMachineState(event);			
	}		
	
	public Optional<MachineOrderMappingStatus> removeMachine(AkkaActorBackedCoreModelAbstractActor machine) {
		return Optional.of(moms.remove(machine));		
	}
	
	public static class MachineOrderMappingStatus {
		AkkaActorBackedCoreModelAbstractActor machine;
		AssignmentState allocationState; 
		MachineUpdateEvent lastMachineState = null;
		String orderId = null;
		ProcessStep productionJob = null;
		
		public MachineOrderMappingStatus(AkkaActorBackedCoreModelAbstractActor machine, AssignmentState allocationState) {
			super();
			this.allocationState = allocationState;
			this.machine = machine;
		}
		
		public AssignmentState getAllocationState() {
			return allocationState;
		}
		public void setAllocationState(AssignmentState allocationState) {
			this.allocationState = allocationState;
		}
		public MachineUpdateEvent getLastMachineState() {
			return lastMachineState;
		}
		public void setLastMachineState(MachineUpdateEvent lastMachineState) {
			if (lastMachineState.getParameterName().equals(STATE_VAR_NAME)) { // only update the state of the machine
				this.lastMachineState = lastMachineState;		
				if (lastMachineState.getNewValue().equals(MachineOrderMappingManager.IDLE_STATE_VALUE)) {
					this.allocationState = AssignmentState.NONE;
					this.orderId = null;
				}
			}
		}
		public String getOrderId() {
			return orderId;
		}
		public void setOrderId(String orderId) {
			this.orderId = orderId;
		}				
		public AkkaActorBackedCoreModelAbstractActor getMachine() {
			return machine;
		}
		public ProcessStep getProductionJob() {
			return this.productionJob;
		}
		public void setProductionJob(ProcessStep step) {
			this.productionJob = step;
		}
		public static enum AssignmentState {
			NONE, REQUESTED, RESERVED, OCCUPIED
		}
	}
}