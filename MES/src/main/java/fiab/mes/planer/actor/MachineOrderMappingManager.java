package fiab.mes.planer.actor;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.basicmachine.events.MachineUpdateEvent;
import fiab.core.capabilities.handshake.HandshakeCapability.ServerSideStates;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.order.msg.OrderEvent;
import fiab.mes.order.msg.OrderEvent.OrderEventType;
import fiab.mes.order.msg.RegisterProcessRequest;
import fiab.mes.planer.actor.MachineOrderMappingManager.MachineOrderMappingStatus.AssignmentState;

public class MachineOrderMappingManager {

    private static final Logger logger = LoggerFactory.getLogger(MachineOrderMappingManager.class);

//	public static final String IDLE_STATE_VALUE = "IDLE";
//	public static final String COMPLETING_STATE_VALUE = "COMPLETING";
//	public static final String PRODUCING_STATE_VALUE = "EXECUTE";
//	public static final String STATE_VAR_NAME = "STATE";

    protected ActorSelection orderEventBus;
    protected String publisherName;
    protected Map<AkkaActorBackedCoreModelAbstractActor, MachineOrderMappingStatus> moms = new HashMap<>();
    protected Map<String, SimpleEntry<RegisterProcessRequest, OrderEventType>> orders = new LinkedHashMap<>(); //we want to check/iterative through orders/processes in sequence of their arrival

    public MachineOrderMappingManager(ActorSelection orderEventBus, String parentActorName) {
        this.orderEventBus = orderEventBus;
        this.publisherName = parentActorName;
    }

    private void publishEvent(String orderId, OrderEventType type, String msg) {
        msg = String.format("Machine-Order-Mapping action: \"%s\" for Order with ID %s", msg, orderId);
        orderEventBus.tell(new OrderEvent(orderId, publisherName, type, msg), ActorRef.noSender());
    }

    public Optional<RegisterProcessRequest> getOrderRequest(String orderId) {
        if (orders.containsKey(orderId)) {
            return Optional.of(orders.get(orderId).getKey());
        } else return Optional.empty();
    }

    public Optional<OrderEventType> getLastOrderState(String orderId) {
        if (orders.containsKey(orderId)) {
            return Optional.of(orders.get(orderId).getValue());
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
        String msg = "register order ";
        publishEvent(rpReq.getRootOrderId(), OrderEventType.REGISTERED, msg);
    }

    public void pauseOrder(String orderId) {
        transitionOrder(orderId, OrderEventType.PAUSED);
    }

    private void transitionOrder(String orderId, OrderEventType newState, String message) {
        Optional.ofNullable(orders.get(orderId)).ifPresent(pair -> {
            OrderEventType oldState = pair.getValue();
            if (oldState != newState) {
                pair.setValue(newState);
                String msg = message != null ? message : String.format("transition from %s to %s ", oldState, newState);
                publishEvent(orderId, newState, msg);
            }
        });
    }

    private void transitionOrder(String orderId, OrderEventType newState) {
        transitionOrder(orderId, newState, null);
    }

    public void markOrderRejected(String orderId, String msg) {
        transitionOrder(orderId, OrderEventType.REJECTED, msg);
    }

    public void allocateProcess(String orderId) {
        transitionOrder(orderId, OrderEventType.ALLOCATED);
    }

    public void scheduleProcess(String orderId) {
        transitionOrder(orderId, OrderEventType.SCHEDULED);
    }

    public void markOrderWaitingForTransport(String orderId) {
        transitionOrder(orderId, OrderEventType.TRANSPORT_REQUESTED);
    }

    public void markOrderDeniedTransport(String orderId, String msg) {
        transitionOrder(orderId, OrderEventType.TRANSPORT_DENIED, msg);
    }

    public void markOrderInTransit(String orderId) {
        transitionOrder(orderId, OrderEventType.TRANSPORT_IN_PROGRESS);
    }

    public void markOrderCompletedTransport(String orderId) {
        transitionOrder(orderId, OrderEventType.TRANSPORT_COMPLETED);
    }

    public void markOrderFailedTransport(String orderId, String msg) {
        transitionOrder(orderId, OrderEventType.TRANSPORT_FAILED, msg);
    }

    public void markOrderCompleted(String orderId, String msg) {
        transitionOrder(orderId, OrderEventType.COMPLETED, msg);
    }

    public void markOrderProducing(String orderId) {
        transitionOrder(orderId, OrderEventType.PRODUCING);
    }

    public void markOrderCanceled(String orderId, String msg) {
        transitionOrder(orderId, OrderEventType.CANCELED, msg);
    }

    public void markOrderRemovedFromShopfloor(String orderId, String msg) {
        transitionOrder(orderId, OrderEventType.REMOVED, msg);
    }

    public void markOrderPrematureRemovalFromShopfloor(String orderId, String msg) {
        transitionOrder(orderId, OrderEventType.PREMATURE_REMOVAL, msg);
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
                .filter(mapping -> mapping.getAssignmentState().equals(AssignmentState.OCCUPIED))
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
                .filter(mom -> mom.getOrderId() != null)
                .filter(mom -> mom.getOrderId().contentEquals(orderId))                        // will also select inputstation when first step in this order
                .filter(mom -> mom.getAssignmentState().equals(AssignmentState.OCCUPIED))
                .map(mom -> mom.getMachine())
                .findFirst();
    }

    public void updateOrderLocation(String orderId, AkkaActorBackedCoreModelAbstractActor newLocation) {
        moms.values().stream()
                .filter(m -> m.getOrderId() != null && m.getOrderId().equals(orderId))
                .findFirst()
                .ifPresent(m -> {
                    //m.setOrderId(null);
                    m.setAllocationState(AssignmentState.CLEANUP);
                });
        moms.keySet().stream()
                .filter(m -> m.equals(newLocation))
                .findFirst()
                .ifPresent(m -> {
                    moms.get(m).setOrderId(orderId);
                    moms.get(m).setAllocationState(AssignmentState.OCCUPIED);
                });

    }

    public Optional<AkkaActorBackedCoreModelAbstractActor> getRequestedMachineOfOrder(String orderId) {
        return moms.values().stream()
                .filter(mom -> mom.getOrderId() != null)
                .filter(mom -> mom.getOrderId().contentEquals(orderId))                        // will also select inputstation when first step in this order
                .filter(mom -> mom.getAssignmentState().equals(AssignmentState.REQUESTED))
                .map(mom -> mom.getMachine())
                .findFirst();
    }

    public boolean isMachineIdle(AkkaActorBackedCoreModelAbstractActor machine) {
        return getIdleMachines().stream()
                .filter(idleM -> idleM.equals(machine))
                .findFirst()
                .isPresent();
    }

    public List<AkkaActorBackedCoreModelAbstractActor> getIdleMachines() {
        return getMachinesInState(BasicMachineStates.IDLE);
    }

    public List<AkkaActorBackedCoreModelAbstractActor> getMachinesReadyForUnloading() {
        return getMachinesInState(BasicMachineStates.COMPLETING);
    }

    private List<AkkaActorBackedCoreModelAbstractActor> getMachinesInState(BasicMachineStates state) {
        return moms.values().stream()
                .filter(mapping -> {
                    return mapping.getLastMachineState().getValue().toString().equals(state.toString()); // we match only Machines with MachineStatus but not IOStations
                })
                .map(mapping -> mapping.getMachine())
                .collect(Collectors.toList());
    }

    public void freeUpMachine(AkkaActorBackedCoreModelAbstractActor machine, boolean waitForNewState) {
        Optional.of(moms.get(machine)).ifPresent(mom -> {
            mom.setOrderId(null);
            mom.setProductionJob(null);
            if (waitForNewState) {
                mom.setAllocationState(AssignmentState.UNKNOWN);
            } else {
                mom.setAllocationState(AssignmentState.NONE);
            }
        });
    }

    public void requestMachineForOrder(AkkaActorBackedCoreModelAbstractActor machine, String orderId, ProcessStep productionJob) throws MachineOrderMappingStatusLifecycleException {

        MachineOrderMappingStatus mom = moms.get(machine);
        if (mom != null) {
            AssignmentState newType = AssignmentState.REQUESTED;
            AssignmentState old = mom.getAssignmentState();
            if (old != AssignmentState.NONE) {
                String errMsg = String.format("Error trying to set AssignmentState to Requested on non-Idle state (%s) for %s", old, orderId);
                logger.warn(errMsg);
                throw new MachineOrderMappingStatusLifecycleException(errMsg);
            }
            mom.setAllocationState(newType);
            mom.setOrderId(orderId);
            mom.setProductionJob(productionJob);
            logger.info(String.format("Updating OrderMapping for order %s on machine %s from %s to %s", mom.getOrderId(), machine.getId(), old, newType));
        }
        ;
    }


    public void reserveOrderAtMachine(AkkaActorBackedCoreModelAbstractActor machine) {
        updateMachineMappingStatus(machine, AssignmentState.RESERVED);
    }

    public void confirmOrderAtMachine(AkkaActorBackedCoreModelAbstractActor machine) {
        updateMachineMappingStatus(machine, AssignmentState.OCCUPIED);
    }

    public void removeOrderAllocationIfMachineStillOccupied(AkkaActorBackedCoreModelAbstractActor machine) {
        Optional.of(moms.get(machine)).ifPresent(mom -> {
            if (mom.getAssignmentState().equals(AssignmentState.OCCUPIED)) {
                updateMachineMappingStatus(machine, AssignmentState.UNKNOWN); //we wait for the machine to signal idle => NONE
            }
        });
    }

    private void updateMachineMappingStatus(AkkaActorBackedCoreModelAbstractActor machine, AssignmentState type) {
        Optional.ofNullable(moms.get(machine)).ifPresent(mom -> {
            logger.debug(String.format("Updating OrderMapping for order %s from %s to %s", mom.getOrderId(), mom.getAssignmentState(), type));
            mom.setAllocationState(type);
        });
    }

    public BasicMachineStates getMachineStatus(AkkaActorBackedCoreModelAbstractActor machine) {
        if (moms.containsKey(machine)) {
            MachineOrderMappingStatus mom = moms.get(machine);
            if (mom.getLastMachineState() != null && mom.getLastMachineState() instanceof MachineStatusUpdateEvent) {
                return ((MachineStatusUpdateEvent) mom.getLastMachineState()).getStatus();
            }
        }
        return BasicMachineStates.UNKNOWN;
    }

    public ServerSideStates getIOStatus(AkkaActorBackedCoreModelAbstractActor machine) {
        if (moms.containsKey(machine)) {
            MachineOrderMappingStatus mom = moms.get(machine);
            if (mom.getLastMachineState() != null && mom.getLastMachineState() instanceof IOStationStatusUpdateEvent) {
                return ((IOStationStatusUpdateEvent) mom.getLastMachineState()).getStatus();
            }
        }
        return ServerSideStates.UNKNOWN;
    }

    public void updateMachineStatus(AkkaActorBackedCoreModelAbstractActor machine, MachineUpdateEvent event) {
        moms.computeIfAbsent(machine, k -> new MachineOrderMappingStatus(machine, AssignmentState.UNKNOWN))
                .setLastMachineState(event);
    }

    public Optional<MachineOrderMappingStatus> removeMachine(AkkaActorBackedCoreModelAbstractActor machine) {
        return Optional.ofNullable(moms.remove(machine));
    }

    public Optional<MachineOrderMappingStatus> getMappingStatusOfMachine(String machineId) {
        return moms.values().stream()
                .filter(mom -> mom.machine.getId().equals(machineId))
                .findAny();
    }

    public Optional<MachineOrderMappingStatus> getMappingStatusOfMachine(AkkaActorBackedCoreModelAbstractActor machine) {
        return Optional.ofNullable(moms.get(machine));
    }

    public static class MachineOrderMappingStatusLifecycleException extends Exception {
        private static final long serialVersionUID = 1L;

        public MachineOrderMappingStatusLifecycleException(String errMsg) {
            super(errMsg);
        }
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

        public AssignmentState getAssignmentState() {
            return allocationState;
        }

        public void setAllocationState(AssignmentState allocationState) {
            this.allocationState = allocationState;
        }

        public MachineUpdateEvent getLastMachineState() {
            return lastMachineState;
        }

        public void setLastMachineState(MachineUpdateEvent lastMachineState) {
            if (!isDifferentState(lastMachineState)) return;
            if (lastMachineState instanceof MachineStatusUpdateEvent) {
                if (((MachineStatusUpdateEvent) lastMachineState).getStatus().equals(BasicMachineStates.IDLE)) {
                    this.allocationState = AssignmentState.NONE;
                    this.orderId = null;
                }
                this.lastMachineState = lastMachineState;
            } else if (lastMachineState instanceof IOStationStatusUpdateEvent) {
                if (((IOStationStatusUpdateEvent) lastMachineState).getStatus().equals(ServerSideStates.IDLE_EMPTY) ||
                        ((IOStationStatusUpdateEvent) lastMachineState).getStatus().equals(ServerSideStates.IDLE_LOADED)) {
                    this.allocationState = AssignmentState.NONE;
                    this.orderId = null;
                }
                this.lastMachineState = lastMachineState;
            }
        }

        private boolean isDifferentState(MachineUpdateEvent lastMachineState) {
            if (lastMachineState == null)
                return false;
            if (this.lastMachineState == null)
                return true;
            if (lastMachineState instanceof MachineStatusUpdateEvent) {
                if (((MachineStatusUpdateEvent) lastMachineState).getStatus().equals(((MachineStatusUpdateEvent) this.lastMachineState).getStatus()))
                    return false; // no update upon same state
            }
            if (lastMachineState instanceof IOStationStatusUpdateEvent) {
                if (((IOStationStatusUpdateEvent) lastMachineState).getStatus().equals(((IOStationStatusUpdateEvent) this.lastMachineState).getStatus()))
                    return false; // no update upon same state
            }
            return true;
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
            UNKNOWN, NONE, REQUESTED, RESERVED, OCCUPIED, CLEANUP
        }
    }
}
