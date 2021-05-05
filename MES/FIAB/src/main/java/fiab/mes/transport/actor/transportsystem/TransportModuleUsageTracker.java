package fiab.mes.transport.actor.transportsystem;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.transport.TransportModuleCapability;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.planer.actor.MachineCapabilityManager;
import fiab.mes.planer.actor.MachineOrderMappingManager.MachineOrderMappingStatusLifecycleException;
import fiab.mes.transport.actor.transportsystem.TransportModuleUsageTracker.TransportModuleOrderMappingStatus.AllocationState;

public class TransportModuleUsageTracker {

	private static final Logger logger = LoggerFactory.getLogger(TransportModuleUsageTracker.class);
	protected Map<AkkaActorBackedCoreModelAbstractActor,TransportModuleOrderMappingStatus> moms = new HashMap<>();
	// helps to track whether we use a transport module, its state, etc
	
	public void requestTransportModuleForOrder(AkkaActorBackedCoreModelAbstractActor machine, String orderId, String requestId) throws MachineOrderMappingStatusLifecycleException 
	{		
		TransportModuleOrderMappingStatus mom = moms.get(machine);
		if (mom != null) { 
			TransportModuleOrderMappingStatus.AllocationState newType = TransportModuleOrderMappingStatus.AllocationState.REQUESTED;
			TransportModuleOrderMappingStatus.AllocationState old = mom.getAssignmentState();
			if (old != TransportModuleOrderMappingStatus.AllocationState.AVAILABLE) {
				String errMsg = String.format("Error trying to set AssignmentState to Requested on non-Idle state (%s) for %s, ignoring request", old, orderId);
				logger.warn(errMsg);
				throw new MachineOrderMappingStatusLifecycleException(errMsg);
			}
			mom.setAllocationState(newType);
			mom.setOrderId(orderId);
			logger.debug(String.format("Updating TransportOrderMapping for order %s from %s to %s", mom.getOrderId(), old, newType));			
		};					
	}
	
	public void unrequestTransportModule(AkkaActorBackedCoreModelAbstractActor machine, String orderId) throws MachineOrderMappingStatusLifecycleException {
		TransportModuleOrderMappingStatus mom = moms.get(machine);
		if (mom != null) { 
			TransportModuleOrderMappingStatus.AllocationState newType = TransportModuleOrderMappingStatus.AllocationState.AVAILABLE;
			TransportModuleOrderMappingStatus.AllocationState old = mom.getAssignmentState();
			if (old != TransportModuleOrderMappingStatus.AllocationState.REQUESTED) {
				String errMsg = String.format("Error trying to unrequesting AssignmentState to NONE from non-REQUESTED state (%s) for %s, ignoring request", old, orderId);
				logger.warn(errMsg);
				throw new MachineOrderMappingStatusLifecycleException(errMsg);
			}
			if (mom.getOrderId().equals(orderId)) {
				mom.setAllocationState(newType);
				mom.setOrderId(orderId);
				logger.debug(String.format("Updating TransportOrderMapping for order %s from %s to %s", mom.getOrderId(), old, newType));		
			} else {
				String errMsg = String.format("Error trying to unrequesting AssignmentState to NONE in state (%s) for mismatching orderId %s, ignoring request", old, orderId);
				logger.warn(errMsg);
				throw new MachineOrderMappingStatusLifecycleException(errMsg);
			}
		};					
	}
	
	public List<AkkaActorBackedCoreModelAbstractActor> getTransportModulesInvolvedInOrder(String orderId) {
		return moms.values().stream()
			.filter(tmoms -> tmoms.getOrderId() != null)
			.filter(tmoms -> tmoms.getOrderId().equals(orderId))
			.map(tmoms -> tmoms.getMachine())
			.collect(Collectors.toList());
	}
	
	public List<AkkaActorBackedCoreModelAbstractActor> getIdleTransportModules() {
		return moms.values().stream()
				.filter(tmoms -> tmoms.getAssignmentState().equals(AllocationState.AVAILABLE))
				.map(tmoms -> tmoms.getMachine())
				.collect(Collectors.toList());
	}
	
	public List<AkkaActorBackedCoreModelAbstractActor> getKnownTransportModules() {
		return moms.values().stream()				
				.map(tmoms -> tmoms.getMachine())
				.collect(Collectors.toList());
	}
	
	public Optional<TransportModuleOrderMappingStatus.AllocationState> getUsageState(AkkaActorBackedCoreModelAbstractActor actor) {
		return Optional.ofNullable(moms.get(actor)).map(tmoms -> 
		tmoms.getAssignmentState());
	}
	
	public void trackIfTransportModule(MachineConnectedEvent event) {
		// if not tracked yet:
		if (moms.values().stream().noneMatch(tmoms -> tmoms.getMachine().equals(event.getMachine()))) {
			//check if one of the capabilities describe a transport module
			if (event.getProvidedMachineCapabilities().stream()
				.flatMap(cap -> MachineCapabilityManager.flatHierarchyToCapsWithNonNullId(cap).stream())
				.filter(cap -> cap.getUri() != null)
				.anyMatch(cap -> cap.getUri().equals(TransportModuleCapability.getTransportCapability().getUri()))) {
				moms.put(event.getMachine(), new TransportModuleOrderMappingStatus(event.getMachine(), AllocationState.NONE));
				logger.info("Registering new TransportModule: "+event.getMachineId());	
			}
		}
			
	}
	
	public Optional<AbstractMap.SimpleEntry<String,String>> updateIfExists(MachineStatusUpdateEvent update) {
		return moms.values().stream()
			.filter(tmoms -> tmoms.getMachine().getId().equals(update.getMachineId()))
			.map(tmoms -> tmoms.setLastMachineState(update))
			.filter(prevValue -> prevValue.isPresent())
			.findAny()			
			.orElse(Optional.empty());
	}

	public boolean isTransportModule(AkkaActorBackedCoreModelAbstractActor actor ) {
		return moms.containsKey(actor);
	}
	
	
	public static class TransportModuleOrderMappingStatus {
		AkkaActorBackedCoreModelAbstractActor machine;
		AllocationState allocationState; 
		MachineStatusUpdateEvent lastMachineState = null;
		String orderId = null;		
		String requestId = null;
		
		public TransportModuleOrderMappingStatus(AkkaActorBackedCoreModelAbstractActor machine, AllocationState allocationState) {
			super();
			this.allocationState = allocationState;
			this.machine = machine;
		}
		
		public AllocationState getAssignmentState() {
			return allocationState;
		}
		public void setAllocationState(AllocationState allocationState) {
			this.allocationState = allocationState;
		}
		public MachineStatusUpdateEvent getLastMachineState() {
			return lastMachineState;
		}
		public Optional<AbstractMap.SimpleEntry<String,String>> setLastMachineState(MachineStatusUpdateEvent lastMachineState) {
			if (lastMachineState.getParameterName().equals(OPCUABasicMachineBrowsenames.STATE_VAR_NAME)) { // only update the state of the machine
				if (!isDifferentState(lastMachineState)) // no update on same state
					return Optional.empty(); 
				else {
					this.lastMachineState = lastMachineState;		
					if (lastMachineState.getStatus().equals(BasicMachineStates.IDLE) //||  only when idle, can we asked for new request
							//lastMachineState.getStatus().equals(MachineStatus.COMPLETE) ||
							//lastMachineState.getStatus().equals(MachineStatus.STOPPED)
							){
						this.allocationState = AllocationState.AVAILABLE;
						AbstractMap.SimpleEntry<String, String> prevValues = new AbstractMap.SimpleEntry<>(this.orderId, this.requestId); 
						this.orderId = null;
						this.requestId = null;
						return Optional.of(prevValues);
					}
				}}
			return Optional.empty();
		}
		
		private boolean isDifferentState(MachineStatusUpdateEvent lastMachineState) {
			if (lastMachineState == null) return false;
			if (this.lastMachineState == null) return true;
			if (this.lastMachineState.getStatus().equals(lastMachineState.getStatus()))
				return false;
			else return true;
		}
		public String getOrderId() {
			return orderId;
		}
		public void setOrderId(String orderId) {
			this.orderId = orderId;
		}		
					
		public String getRequestId() {
			return requestId;
		}

		public void setRequestId(String requestId) {
			this.requestId = requestId;
		}

		public AkkaActorBackedCoreModelAbstractActor getMachine() {
			return machine;
		}

		public static enum AllocationState {
			NONE, AVAILABLE, REQUESTED, OCCUPIED
		}
	}
	
}
