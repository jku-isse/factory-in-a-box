package fiab.mes.productioncell.foldingstation;

import ProcessCore.AbstractCapability;
import ProcessCore.CapabilityInvocation;
import ProcessCore.ProcessCoreFactory;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.basicmachine.events.MachineEvent.MachineEventType;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.folding.WellknownFoldingCapability;
import fiab.core.capabilities.handshake.HandshakeCapability.ServerSideStates;
import fiab.core.capabilities.handshake.IOStationCapability;
import fiab.core.capabilities.transport.TransportModuleCapability;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.machine.msg.MachineDisconnectedEvent;
import fiab.mes.machine.msg.OrderRelocationNotification;
import fiab.mes.order.OrderProcess;
import fiab.mes.order.OrderProcess.ProcessChangeImpact;
import fiab.mes.order.msg.CancelOrTerminateOrder;
import fiab.mes.order.msg.OrderEvent.OrderEventType;
import fiab.mes.order.msg.OrderProcessUpdateEvent;
import fiab.mes.order.msg.RegisterProcessRequest;
import fiab.mes.order.msg.RegisterProcessStepRequest;
import fiab.mes.planer.actor.MachineCapabilityManager;
import fiab.mes.planer.actor.MachineOrderMappingManager;
import fiab.mes.planer.actor.MachineOrderMappingManager.MachineOrderMappingStatus;
import fiab.mes.planer.actor.MachineOrderMappingManager.MachineOrderMappingStatus.AssignmentState;
import fiab.mes.planer.actor.MachineOrderMappingManager.MachineOrderMappingStatusLifecycleException;
import fiab.mes.planer.msg.PlanerStatusMessage;
import fiab.mes.planer.msg.PlanerStatusMessage.PlannerState;
import fiab.mes.transport.actor.transportsystem.TransportSystemCoordinatorActor;
import fiab.mes.transport.msg.CancelTransportRequest;
import fiab.mes.transport.msg.RegisterTransportRequest;
import fiab.mes.transport.msg.RegisterTransportRequestStatusResponse;
import fiab.mes.transport.msg.TransportSystemStatusMessage;

import java.util.*;
import java.util.stream.Collectors;


public class FoldingProductionCellCoordinator extends AbstractActor{

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static final String WELLKNOWN_LOOKUP_NAME = "FoldingProductionCellCoordinator";

    // manages which machine has which capabilities
    protected MachineCapabilityManager capMan = new MachineCapabilityManager();

    // manages which machines has currently which order allocated and in which state
    protected MachineOrderMappingManager ordMapper;

    protected ActorSelection orderEventBus;
    protected ActorSelection machineEventBus;
    protected ActorSelection transportCoordinator;
    protected ActorRef self;
    protected PlannerState state = PlannerState.STOPPED;
    protected TransportSystemStatusMessage.State tsysState = TransportSystemStatusMessage.State.STOPPED;

    static public Props props() {
        return Props.create(FoldingProductionCellCoordinator.class, () -> new FoldingProductionCellCoordinator());
    }

    public FoldingProductionCellCoordinator() {
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
                //.match(CancelOrTerminateOrder.class, req -> {
                //    handleCancelOrderRequest(req);
                //})
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
                .match(OrderRelocationNotification.class, notification ->{
                    this.ordMapper.updateOrderLocation(notification.getOrderId(), notification.getTargetActor());
                })
                .match(TransportSystemStatusMessage.class, msg -> {
                    this.tsysState = msg.getState();
                    checkPlannerState();
                })
                .match(RegisterProcessRequest.class, msg -> {/*ignore*/})
                .build();
    }

    private void getEventBusAndSubscribe() throws Exception{
        SubscribeMessage orderSub = new SubscribeMessage(getSelf(), new MESSubscriptionClassifier(WELLKNOWN_LOOKUP_NAME, "*"));
        orderEventBus = this.context().actorSelection("/user/"+OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        orderEventBus.tell(orderSub, getSelf());

        SubscribeMessage machineSub = new SubscribeMessage(getSelf(), new MESSubscriptionClassifier(WELLKNOWN_LOOKUP_NAME, "*"));
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

    // this method will trigger order to machine allocation when there is a machine already idle,
    // if there are all machines occupied, then this will not do anything (order will be paused)
    private void tryAssignExecutingMachineForOneProcessStep(OrderProcess op, String rootOrderId) {
        // basically we get possible steps from process (filter out flow control elements) and register first step at machine
        CapabilityInvocation foldingCap = ProcessCoreFactory.eINSTANCE.createCapabilityInvocation();
        foldingCap.setID("FoldingCapabilityId");
        foldingCap.setDisplayName("FoldingCapability");
        foldingCap.setInvokedCapability(WellknownFoldingCapability.getFoldingShapeCapability());
        List<CapabilityInvocation> stepCandidates = Collections.singletonList(foldingCap);

            Map<CapabilityInvocation,Set<AkkaActorBackedCoreModelAbstractActor>> capMap = stepCandidates.stream()
                    .map(step -> new AbstractMap.SimpleEntry<>(step, capMan.getMachinesProvidingCapability(step.getInvokedCapability())) )
                    .filter(pair -> !pair.getValue().isEmpty() )
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (prev, next) -> next, HashMap::new));

            System.out.println("CapMap=" + capMap);

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
        //}
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

    private void handleIOStationUpdateEvent(IOStationStatusUpdateEvent ue) {
        log.info(String.format("IOStationUpdateEvent for machine %s : %s", ue.getMachineId(), ue.getStatus().toString()));
        capMan.resolveById(ue.getMachineId()).ifPresent(machine -> {
            ordMapper.updateMachineStatus(machine, ue);
            if (ue.getStatus().equals(ServerSideStates.IDLE_EMPTY)) {
                // we reached idle for an outputstation
                ordMapper.getProcessesInState(OrderEventType.COMPLETED).stream()
                        .forEach(rpr -> tryAssignExecutingMachineForOneProcessStep(rpr.getProcess(), rpr.getRootOrderId()));
                ordMapper.getProcessesInState(OrderEventType.CANCELED).stream() //orders that need to be prematurely removed
                        .forEach(rpr -> tryAssignExecutingMachineForOneProcessStep(rpr.getProcess(), rpr.getRootOrderId()));
                //tryAssigningTransportToOutputStation();
            } else if (ue.getStatus().equals(ServerSideStates.IDLE_LOADED)) {
                tryAssigningTransportToFoldingStation();
                // we reached idle for an inputstation
                // we are ready to start a new process as this input is ready
                /*ordMapper.getProcessesInState(OrderEventType.PAUSED).stream()
                        .forEach(rpr -> tryAssignExecutingMachineForOneProcessStep(rpr.getProcess(), rpr.getRootOrderId()));
                ordMapper.getProcessesInState(OrderEventType.REGISTERED).stream()
                        .forEach(rpr -> tryAssignExecutingMachineForOneProcessStep(rpr.getProcess(), rpr.getRootOrderId()));*/
            }
        });
    }

    private void handleMachineUpdateEvent(MachineStatusUpdateEvent mue) {
        log.info(String.format("MachineUpdateEvent for machine %s : %s", mue.getMachineId(), mue.getStatus().toString()));
        capMan.resolveById(mue.getMachineId()).ifPresent(machine -> {
            //MachineStatus prevState = ordMapper.getMachineStatus(machine);
            ordMapper.updateMachineStatus(machine, mue);
            switch(mue.getStatus()) {
                case RESETTING:
                    break;
                case IDLE:
                    tryAssigningTransportToFoldingStation();
                    //tryAssignExecutingMachineForOneProcessStep(null, null);
                    // if idle --> machine ready --> lets check if any order is waiting for that machine
                    /*ordMapper.getPausedProcessesOnSomeMachine().stream()
                            .forEach(rpr -> tryAssignExecutingMachineForOneProcessStep(rpr.getProcess(), rpr.getRootOrderId()));
                    // we don't abort upon first success but try for every one, shouldn't matter, just takes a bit of processing

                    // order that are paused but not assigned to any machine yet (not efficient as will check all those already checked above)
                    ordMapper.getProcessesInState(OrderEventType.PAUSED).stream()
                            .forEach(rpr -> tryAssignExecutingMachineForOneProcessStep(rpr.getProcess(), rpr.getRootOrderId()));
*/
                    //if none, then just wait for next incoming event
                    break;
                case STARTING:
                    tryAssigningTransportToFoldingStation();
                    break;
                case EXECUTE:
                    // now update mapping that order has arrived at that machine and is loaded
                    ordMapper.confirmOrderAtMachine(machine);
                    ordMapper.getOrderRequestOnMachine(machine).ifPresent(rpr -> {
                        ordMapper.markOrderProducing(rpr.getRootOrderId());
                    });
                    break;
                case COMPLETE:
                    break;
                case COMPLETING:
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
                    //tryAssigningTransportToOutputStation();
                    break;
                case STOPPING:// fallthrough
                case STOPPED:
                    handleStoppingOrStoppedMachine(machine);
                    break;
                case UNKNOWN:
                    break;
                default:
                    break;
            }
        });
    }

    private void tryAssigningTransportToFoldingStation(){
        //TODO fix bug where transport only occurs between io and second folding station
        //Find InputStation in Idle Loaded
        Set<AkkaActorBackedCoreModelAbstractActor> availableInputStations = capMan.getMachinesProvidingCapability(inputStationCap);
        Optional<AkkaActorBackedCoreModelAbstractActor> idleInputStation = availableInputStations.stream()
                .filter(m -> ordMapper.getIOStatus(m).equals(ServerSideStates.IDLE_LOADED))
                .findAny();
        //Find FoldingStation in Starting
        Set<AkkaActorBackedCoreModelAbstractActor> availableFoldingStations = capMan.getMachinesProvidingCapability(foldingStationCap);
        Optional<AkkaActorBackedCoreModelAbstractActor> readyFoldingStation = availableFoldingStations.stream()
                .filter(m -> ordMapper.getMachineStatus(m).equals(BasicMachineStates.STARTING))
                .findAny();
        availableFoldingStations.forEach(System.out::println);
        List<AkkaActorBackedCoreModelAbstractActor> readyFoldingStationList = availableFoldingStations.stream()
                .filter(m -> ordMapper.getMachineStatus(m).equals(BasicMachineStates.STARTING))
                .collect(Collectors.toList());
        readyFoldingStationList.forEach(s -> System.out.println(s.getId()+" is in State: "+ordMapper.getMachineStatus(s)));
        //Check if transport FU is available
        Set<AkkaActorBackedCoreModelAbstractActor> availableTransportFus = capMan.getMachinesProvidingCapability(transportCap);
        Optional<AkkaActorBackedCoreModelAbstractActor> idleTransportFU = availableTransportFus.stream()
                .filter(m -> ordMapper.getMachineStatus(m).equals(BasicMachineStates.IDLE))
                .findAny();

        if (idleInputStation.isPresent() && readyFoldingStation.isPresent() && idleTransportFU.isPresent()) {
            log.info("Sending TransportRequest from: " + idleInputStation.get().getId() + ", to:" + readyFoldingStation.get().getId() + ", using transportFU " + idleTransportFU.get().getId());
            transportCoordinator.tell(new RegisterTransportRequest(idleInputStation.get(), readyFoldingStation.get(),
                    ordMapper.getJobOnMachine(idleInputStation.get()).toString(), self), self);
        } else {
            log.info("Waiting for Machines to be in suitable state. Machines in suitable state? IO: {}, FoldingStation: {}, Transport: {}", idleInputStation.isPresent(), readyFoldingStation.isPresent(), idleTransportFU.isPresent());
            //System.out.println("Available Input station " + idleInputStation + ", Available Folding Station: " + readyFoldingStation + ", TransportFU: " + idleTransportFU);
        }
    }

    /*private void tryAssigningTransportToOutputStation(){
        //Find OutputStation in Idle Empty
        Set<AkkaActorBackedCoreModelAbstractActor> availableOutputStations = capMan.getMachinesProvidingCapability(outputStationCap);
        Optional<AkkaActorBackedCoreModelAbstractActor> idleOutputStation = availableOutputStations.stream()
                .filter(m -> ordMapper.getIOStatus(m).equals(ServerSideStates.IDLE_EMPTY))
                .findAny();
        //Find FoldingStation in Completing
        Set<AkkaActorBackedCoreModelAbstractActor> availableFoldingStations = capMan.getMachinesProvidingCapability(foldingStationCap);
        Optional<AkkaActorBackedCoreModelAbstractActor> readyFoldingStation = availableFoldingStations.stream()
                .filter(m -> ordMapper.getMachineStatus(m).equals(BasicMachineStates.COMPLETING))
                .findAny();
        availableFoldingStations.forEach(System.out::println);
        List<AkkaActorBackedCoreModelAbstractActor> readyFoldingStationList = availableFoldingStations.stream()
                .filter(m -> ordMapper.getMachineStatus(m).equals(BasicMachineStates.COMPLETING))
                .collect(Collectors.toList());
        readyFoldingStationList.forEach(s -> System.out.println(s.getId()+" is in State: "+ordMapper.getMachineStatus(s)));
        //Check if transport FU is available
        Set<AkkaActorBackedCoreModelAbstractActor> availableTransportFus = capMan.getMachinesProvidingCapability(transportCap);
        Optional<AkkaActorBackedCoreModelAbstractActor> idleTransportFU = availableTransportFus.stream()
                .filter(m -> ordMapper.getMachineStatus(m).equals(BasicMachineStates.IDLE))
                .findAny();
        if (idleOutputStation.isPresent() && readyFoldingStation.isPresent() && idleTransportFU.isPresent()) {
            log.info("Sending TransportRequest from: " + idleOutputStation.get().getId() + ", to:" + readyFoldingStation.get().getId() + ", using transportFU " + idleTransportFU.get().getId());
            transportCoordinator.tell(new RegisterTransportRequest(idleOutputStation.get(), readyFoldingStation.get(), "missingOrderIdFromCoordinator", self), self);
        } else {
            log.info("Waiting for Machines to be in suitable state. Machines in suitable state? IO: {}, FoldingStation: {}, Transport: {}", idleOutputStation.isPresent(), readyFoldingStation.isPresent(), idleTransportFU.isPresent());
            //System.out.println("Available Input station " + idleInputStation + ", Available Folding Station: " + readyFoldingStation + ", TransportFU: " + idleTransportFU);
        }
    }*/

    private void handleStoppingOrStoppedMachine(AkkaActorBackedCoreModelAbstractActor machine) {
        // this is called when the machine is explicitly stopped, e.g. to shut it down or because of emergency,
        // we now need to check how we use this machine in the scheduling
        ordMapper.getMappingStatusOfMachine(machine.getId()).ifPresent(moms -> {
            switch(moms.getAssignmentState())
            {
                case UNKNOWN: //fallthrough
                case NONE:
                    break;
                case REQUESTED: //we asked machine but it has not responded, thus order still in previous location
                    log.info(String.format("Freeing up STOPPING/STOPPED Machine %s originally REQUESTED for order %s ", machine.getId(), moms.getOrderId()));
                    ordMapper.pauseOrder(moms.getOrderId());
                    ordMapper.freeUpMachine(machine, true);
                    break;
                case RESERVED: // we got a positive response to work and perhaps asked for transport
                    log.info(String.format("Freeing up STOPPING/STOPPED Machine %s originally RESERVED for order %s ", machine.getId(), moms.getOrderId()));
                    // now we need to check if order is (about to be) in transit
                    ordMapper.getLastOrderState(moms.getOrderId()).ifPresent(state2 -> {
                        log.info(String.format("Checking potential Transport Progress state %s for RESERVED order %s ", state2, moms.getOrderId()));
                        if (state2.equals(OrderEventType.TRANSPORT_REQUESTED) || state2.equals(OrderEventType.TRANSPORT_IN_PROGRESS)) {
                            // cancel transport, we might be in state requested, then we may continue, but transport might just have started
                            transportCoordinator.tell(new CancelTransportRequest(moms.getOrderId()), self);
                            // transport will tell us whether it started already, we then react to these transport update events
                            // so we don't mark it at all, if not started then we can continue it elsewhere
                        }
                    });
                    ordMapper.freeUpMachine(machine, true);
                    break;
                case OCCUPIED: // the machine is working on this order, cancel Order, then manual removal needed
                    //order might completed and about to be in transit
                    ordMapper.getLastOrderState(moms.getOrderId()).ifPresent(state -> {
                        log.info(String.format("Checking potential Transport Progress state %s for OCCUPIED order %s ", state, moms.getOrderId()));
                        if (state.equals(OrderEventType.TRANSPORT_REQUESTED) || state.equals(OrderEventType.TRANSPORT_IN_PROGRESS)) {
                            // ensure, the transport is canceled, transportmodules are freed up
                            transportCoordinator.tell(new CancelTransportRequest(moms.getOrderId()), self);
                        }
                    });
                    ordMapper.removeOrderAllocationIfMachineStillOccupied(machine);
                    ordMapper.markOrderPrematureRemovalFromShopfloor(moms.getOrderId(), "Machine stopped unexpectedly, please manally remove order from machine: "+machine.getId());
                    break;
                default:
                    break;
            }
        }); // if not present, then nothing to do
        // if this is called a second time (after stopping) then also in stopped, nothing will happen as the ordMapper has set the machine to Unknown, or None
    }

    private void handleNoLongerAvailableMachine(MachineDisconnectedEvent mde) {
        // handling similar to stopping machine
        handleStoppingOrStoppedMachine(mde.getMachine());
        // now we can remove the machine
        Optional<Set<AbstractCapability>> remCap = capMan.removeActor(mde.getMachine());
        remCap.ifPresent(cap -> log.info("Removed Capabilities for machine: "+mde.getMachineId()));
        ordMapper.removeMachine(mde.getMachine());
        checkPlannerState();
    }

    private void handleNewlyAvailableMachine(MachineConnectedEvent mce) {
        capMan.setCapabilities(mce);
        log.info("Storing Capabilities for machine: "+mce.getMachineId());
        //check for input/output stations
        checkPlannerState();
        // now wait for machine available event to make use of it (currently we dont know its state)
    }

    protected AbstractCapability inputStationCap = IOStationCapability.getInputStationCapability();
    protected AbstractCapability outputStationCap = IOStationCapability.getOutputStationCapability();
    protected AbstractCapability foldingStationCap = WellknownFoldingCapability.getFoldingShapeCapability();
    protected AbstractCapability transportCap = TransportModuleCapability.getTransportCapability();

    private void checkPlannerState() {
        PlannerState currState = state;
        PlannerState newState = PlannerState.STOPPED;
        if (capMan.getMachinesProvidingCapability(inputStationCap).size() > 0 &&
                capMan.getMachinesProvidingCapability(outputStationCap).size() > 0 &&
                tsysState.equals(TransportSystemStatusMessage.State.FULLY_OPERATIONAL)) {
            newState = PlannerState.FULLY_OPERATIONAL;
        } else {
            newState = PlannerState.DEGRADED_MODE;
        }
        if (newState != currState) {
            state = newState;
            if (newState.equals(PlannerState.DEGRADED_MODE))
                log.warning(String.format("Switched State to %s", state));
            else
                log.info(String.format("Switched State to %s", state));
            publishLocalState(MachineEventType.UPDATED, state, "Input/Outputstation/Transportsystem availability changed");
        }
    }

    private void handleResponseToTransportRequest(RegisterTransportRequestStatusResponse resp) {
        String orderId = resp.getOriginalRequest().getOrderId();
        String msg = "";
        switch(resp.getResponse()) {
            case COMPLETED:
                ordMapper.getLastOrderState(orderId).ifPresent(state -> {
                    if (state.equals(OrderEventType.CANCELED)) { // order at new machine, remove there and reset machine
                        String msg2 = String.format("Order was canceled please remove manually from destination machine and reset destination machine");
                        log.warning(msg2);
                        ordMapper.markOrderPrematureRemovalFromShopfloor(orderId, msg2);
                    }
                }); // else we assume order can be rerouted to another machine later

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
                            ordMapper.freeUpMachine(machine2, false);
                            ordMapper.markOrderRemovedFromShopfloor(orderId, "Order left the shopfloor via Outputmachine: "+oStation.getId());
                        });
                // freeup allocation of source machine
                ordMapper.removeOrderAllocationIfMachineStillOccupied(resp.getOriginalRequest().getSource());
                break;
            case FAILED_IN_TRANSPORT:
                msg = String.format("Transport of Order %s failed with reason %s : %s", orderId, resp.getResponse(), resp.getMessage());
                ordMapper.markOrderFailedTransport(orderId, msg);
                ordMapper.markOrderPrematureRemovalFromShopfloor(orderId, msg);
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
                ordMapper.markOrderPrematureRemovalFromShopfloor(orderId, msg);
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
            case CANCELED:
                // transport did not happen, request is removed, pallet still at current location, we need to reset state to paused
                // if order at non-stopped machine, then
                ordMapper.getLastOrderState(orderId).ifPresent(state -> {
                    if (state.equals(OrderEventType.CANCELED)) { // order still at machine,
                        // we move to next outputstation when ready
                        ordMapper.getProcessesInState(OrderEventType.CANCELED).stream() //orders that need to be prematurely removed
                                .forEach(rpr -> tryAssignExecutingMachineForOneProcessStep(rpr.getProcess(), rpr.getRootOrderId()));
                        return;
                    }
                }); // else we assume order can be rerouted to another machine later
                ordMapper.pauseOrder(orderId);
                // we wait for next machine update
                break;
            case ABORTED:
                // transport was already under way, pallet needs to be removed manually from current locaton, which may be between machine and turntable
                msg = String.format("Transport was aborted, please remove manually from turntable");
                log.warning(msg);
                ordMapper.markOrderPrematureRemovalFromShopfloor(orderId, msg);
                // TODO: we could have just managed to transport way from a stopped machine, then we would not need to do this, but this needs to be handled in the transport coordinator
                // and this would need better handling in the handleStoppedOrStoppingMachine()
                break;
            default:
                break;
        }
    }
}

