package fiab.mes.transport.actor.transportmodule;

import ActorCoreModel.Actor;
import ProcessCore.AbstractCapability;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.basicmachine.events.MachineInWrongStateResponse;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.basicmachine.events.MachineUpdateEvent;
import fiab.core.capabilities.transport.TransportRequest;
import fiab.functionalunit.connector.FUSubscriptionClassifier;
import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.functionalunit.connector.MachineEventBus;
import fiab.mes.general.HistoryTracker;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.msg.GenericMachineRequests.Reset;
import fiab.mes.machine.msg.GenericMachineRequests.Stop;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.machine.msg.MachineDisconnectedEvent;
import fiab.mes.restendpoint.requests.MachineHistoryRequest;
import fiab.mes.transport.actor.transportmodule.wrapper.TransportModuleWrapperInterface;
import fiab.mes.transport.actor.transportsystem.DefaultTransportPositionLookup;
import fiab.mes.transport.actor.transportsystem.TransportPositionLookupInterface;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface.Position;
import fiab.mes.transport.msg.TransportModuleRequest;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;


public class BasicTransportModuleActor extends AbstractActor {

    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    protected ActorSelection eventBusByRef;
    protected final AkkaActorBackedCoreModelAbstractActor machineId;
    protected AbstractCapability cap;
    protected BasicMachineStates currentState = BasicMachineStates.UNKNOWN;
    protected TransportModuleWrapperInterface hal;
    protected MachineEventBus intraBus;
    protected TransportPositionLookupInterface tpl;
    protected Position selfPos;
    protected InternalCapabilityToPositionMapping icpm;
    protected TransportRequest reservedForTReq = null;
    private ActorRef self;
    private boolean autoComplete = true;

    private HistoryTracker externalHistory;
    //private List<MachineEvent> internalHistory = new ArrayList<MachineEvent>();

    static public Props props(ActorSelection machineEventBus, AbstractCapability cap, Actor modelActor, TransportModuleWrapperInterface hal, Position selfPos, MachineEventBus intraBus, TransportPositionLookupInterface tpl, InternalCapabilityToPositionMapping icpm) {
        return Props.create(BasicTransportModuleActor.class, () -> new BasicTransportModuleActor(machineEventBus, cap, modelActor, hal, selfPos, intraBus, tpl, icpm));
    }

    public BasicTransportModuleActor(ActorSelection machineEventBus, AbstractCapability cap, Actor modelActor, TransportModuleWrapperInterface hal, Position selfPos, MachineEventBus intraBus, TransportPositionLookupInterface tpl, InternalCapabilityToPositionMapping icpm) {
        this.cap = cap;
        this.machineId = new AkkaActorBackedCoreModelAbstractActor(modelActor.getID(), modelActor, self());
        log.info("Starting MESMachineActor for: " + this.machineId);
        this.eventBusByRef = machineEventBus;
        this.hal = hal;
        this.intraBus = intraBus;
        this.tpl = tpl; // later versions will obtain such info dynamically from accessing own capability OPC UA information
        this.icpm = icpm; // later versions will obtain such info dynamically from accessing own wiring OPC UA information
        this.selfPos = selfPos;
        this.self = self();
        this.externalHistory = new HistoryTracker(machineId.getId());
        init();

    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(TransportModuleRequest.class, req -> {
                    log.info("Received TransportModuleRequest from {} to {} for order {}",
                            req.getPosFrom(), req.getPosTo(), req.getOrderId());
                    if (currentState == BasicMachineStates.IDLE) {
                        processTransportModuleRequest(req);
                    } else {
                        String msg = String.format("Received TransportModuleRequest %s in incompatible local state %s", req.getOrderId(), this.currentState);
                        log.warning(msg);
                        getSender().tell(new MachineInWrongStateResponse(machineId.getId(), OPCUABasicMachineBrowsenames.STATE_VAR_NAME, msg, this.currentState, req, BasicMachineStates.IDLE), self());
                    }
                })
                // map from positions to capabilityInstances local to the transport module
                .match(TransportRequest.class, req -> {
                    log.info("Received TransportModuleRequest from {} to {} for order {}",
                            req.getCapabilityInstanceIdFrom(), req.getCapabilityInstanceIdTo(), req.getOrderId());
                    if (currentState.equals(BasicMachineStates.IDLE)) {
                        processTransportRequest(req);
                    } else {
                        String msg = String.format("Received TransportModuleRequest %s in incompatible local state %s", req.getOrderId(), this.currentState);
                        log.warning(msg);
                        getSender().tell(new MachineInWrongStateResponse(machineId.getId(), OPCUABasicMachineBrowsenames.STATE_VAR_NAME, msg, this.currentState, req, BasicMachineStates.IDLE), self());
                    }
                })
                .match(MachineStatusUpdateEvent.class, mue -> {
                    processMachineUpdateEvent(mue);
                })
                .match(MachineHistoryRequest.class, req -> {
                    log.info(String.format("Machine %s received MachineHistoryRequest", machineId.getId()));
                    externalHistory.sendHistoryResponseTo(req, getSender(), self);
                })
                .match(Stop.class, req -> {
                    log.info(String.format("TransportModule %s received StopRequest", machineId.getId()));
                    setAndPublishSensedState(BasicMachineStates.STOPPING);
                    hal.stop();
                })
                .match(Reset.class, req -> {
                    if (currentState.equals(BasicMachineStates.COMPLETE)
                            || currentState.equals(BasicMachineStates.STOPPED)) {
                        log.info(String.format("TransportModule %s received ResetRequest in suitable state", machineId.getId()));
                        setAndPublishSensedState(BasicMachineStates.RESETTING); // not sensed, but machine would do the same (or fail, then we need to wait for machine to respond)
                        hal.reset();
                    } else {
                        log.warning(String.format("TransportModule %s received ResetRequest in non-COMPLETE or non-STOPPED state, ignoring", machineId.getId()));
                    }
                })
                .match(MachineDisconnectedEvent.class, req -> {
                    log.warning(String.format("Lost connection to machine in state: %s, sending disconnected event and shutting down actor", this.currentState));
                    if (!currentState.equals(BasicMachineStates.STOPPED)) {
                        setAndPublishSensedState(BasicMachineStates.STOPPED);
                    }
                    eventBusByRef.tell(new MachineDisconnectedEvent(machineId), self());
                    getContext().stop(getSelf());
                })
                .matchAny(msg -> log.debug("Proxy for {} received unknown message {}", machineId, msg))
                .build();
    }

    @Override
    public void postStop() throws Exception {
        super.postStop();
        intraBus.unsubscribe(self);
    }

    private void init() {
        eventBusByRef.tell(new MachineConnectedEvent(machineId, Collections.singleton(cap), Collections.emptySet()), self());
        intraBus.subscribe(getSelf(), new FUSubscriptionClassifier(machineId.getId(), "*")); //ensure we get all events on this bus, but never our own, should we happen to accidentally publish some
        hal.subscribeToStatus();
    }

    private void processTransportRequest(TransportRequest req) {
        //we check if the requests directions encoded in the capabilities are indeed found on this transportmodule
        Position positionSource = new Position(req.getCapabilityInstanceIdFrom());
        Position positionDestination = new Position(req.getCapabilityInstanceIdTo());
        Optional<String> capFrom = icpm.getCapabilityIdForPosition(positionSource, selfPos);
        Optional<String> capTo = icpm.getCapabilityIdForPosition(positionDestination, selfPos);
        if (capFrom.isPresent() && capTo.isPresent()) {
            setAndPublishSensedState(BasicMachineStates.STARTING);
            reservedForTReq = new TransportRequest(capFrom.get(), capTo.get(), req.getOrderId(), req.getRequestId());
            hal.transport(reservedForTReq);
        } else {
            log.warning(String.format("TransportModuleRequest %s from %s to %s cannot be resolved to local capabilities", req.getOrderId(), positionSource, positionDestination));
            //TODO: return error message to sender
        }
    }


    private void processTransportModuleRequest(TransportModuleRequest req) {
        //we check if the requests directions encoded in the capabilities are indeed found on this transportmodule
        Position positionSource = req.getPosFrom();
        Position positionDestination = req.getPosTo();
        Optional<String> capFrom = icpm.getCapabilityIdForPosition(req.getPosFrom(), selfPos);
        Optional<String> capTo = icpm.getCapabilityIdForPosition(req.getPosTo(), selfPos);
        if (capFrom.isPresent() && capTo.isPresent()) {
            forwardTransportRequest(req, capFrom, capTo);
        } else {
            log.warning(String.format("TransportModuleRequest %s from %s to %s cannot be resolved to local capabilities", req.getOrderId(), positionSource, positionDestination));
            //TODO: return error message to sender
        }
    }

    private void forwardTransportRequest(TransportModuleRequest req, Optional<String> capFrom, Optional<String> capTo) {
        setAndPublishSensedState(BasicMachineStates.STARTING);
        reservedForTReq = new TransportRequest(capFrom.get(), capTo.get(), req.getOrderId(), req.getRequestId());
        hal.transport(reservedForTReq);
    }

    private void processMachineUpdateEvent(MachineStatusUpdateEvent mue) {
        if (mue.getParameterName().equals(OPCUABasicMachineBrowsenames.STATE_VAR_NAME)) {
            BasicMachineStates newState = mue.getStatus();
            setAndPublishSensedState(newState);
            switch (newState) {
                case COMPLETE:
                    if (autoComplete) {
                        reset();
                    }
                    //reservedForTReq = null;
                    break;
                case COMPLETING:
                    break;
                case EXECUTE: // for now we guess to have obtained the pallet for given order, --> this would need to be confirmed by the sub actor representing the turntable
                    break;
                case IDLE:
                    break;
                case RESETTING:
                    break;
                case STARTING:
                    break;
                case STOPPED:
                    if (autoComplete) {
                        reset();
                    }
                    break;
                case STOPPING:
                    break;
                default:
                    break;
            }
        }

    }

    private void setAndPublishSensedState(BasicMachineStates newState) {
        String order = reservedForTReq != null ? reservedForTReq.getOrderId() : "none";
        String msg = String.format("%s sets state from %s to %s (Order: %s)", this.machineId.getId(), this.currentState, newState, order);
        log.debug(msg);
        if (currentState != newState) {
            this.currentState = newState;
            MachineUpdateEvent mue = new MachineStatusUpdateEvent(machineId.getId(), OPCUABasicMachineBrowsenames.STATE_VAR_NAME, msg, newState);
            tellEventBus(mue);
        }
    }

    private void tellEventBus(MachineUpdateEvent mue) {
        externalHistory.add(mue);
        tellEventBusWithoutAddingToHistory(mue);
        lastMUE = mue;
        resendLastEvent();
    }

    private void tellEventBusWithoutAddingToHistory(MachineUpdateEvent mue) {
        eventBusByRef.tell(mue, self);
    }

    private MachineUpdateEvent lastMUE;

    private void resendLastEvent() {
        context().system()
                .scheduler()
                .scheduleOnce(Duration.ofMillis(1000 * 10),
                        new Runnable() {
                            @Override
                            public void run() {
                                tellEventBusWithoutAddingToHistory(lastMUE);
                            }
                        }, context().system().dispatcher());
    }

    private void reset() {
        reservedForTReq = null;
        hal.reset();
    }
}
