package fiab.mes.transport.actor.bufferstation;

import ActorCoreModel.Actor;
import ProcessCore.AbstractCapability;
import akka.actor.AbstractActor;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.basicmachine.events.MachineInWrongStateResponse;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.basicmachine.events.MachineUpdateEvent;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.machine.msg.MachineDisconnectedEvent;
import fiab.mes.restendpoint.requests.MachineHistoryRequest;
import fiab.mes.machine.msg.GenericMachineRequests.Reset;
import fiab.mes.machine.msg.GenericMachineRequests.Stop;
import fiab.mes.transport.actor.bufferstation.msg.LoadRequest;
import fiab.mes.transport.actor.bufferstation.msg.UnloadRequest;
import fiab.mes.transport.actor.bufferstation.wrapper.BufferStationWrapperInterface;
import fiab.mes.transport.msg.TransportModuleRequest;
import fiab.turntable.actor.InternalTransportModuleRequest;
import fiab.turntable.actor.IntraMachineEventBus;
import fiab.mes.general.HistoryTracker;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.turntable.actor.SubscriptionClassifier;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;

public class BasicBufferStationActor extends AbstractActor {


    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    protected ActorSelection eventBusByRef;
    protected final AkkaActorBackedCoreModelAbstractActor machineId;
    protected AbstractCapability cap; //capability as turntable, but switch to buffer
    protected IntraMachineEventBus intraBus;
    protected BasicMachineStates currentState;
    protected BufferStationWrapperInterface hal;
    private MachineUpdateEvent lastMUE;
    protected String orderId = null;

    private HistoryTracker externalHistory;

    public static Props props(ActorSelection machineEventBus, AbstractCapability cap, Actor modelActor, IntraMachineEventBus intraBus, BufferStationWrapperInterface hal) {
        return Props.create(BasicBufferStationActor.class, () -> new BasicBufferStationActor(machineEventBus, cap, modelActor, intraBus, hal));
    }

    public BasicBufferStationActor(ActorSelection machineEventBus, AbstractCapability cap, Actor modelActor, IntraMachineEventBus intraBus, BufferStationWrapperInterface hal) {
        this.cap = cap;
        this.machineId = new AkkaActorBackedCoreModelAbstractActor(modelActor.getID(), modelActor, self());
        this.eventBusByRef = machineEventBus;
        this.intraBus = intraBus;
        this.hal = hal;
        init();
    }

    private void init() {
        eventBusByRef.tell(new MachineConnectedEvent(machineId, Collections.singleton(cap), Collections.emptySet()), self());
        intraBus.subscribe(getSelf(), new SubscriptionClassifier(machineId.getId(), "*")); //ensure we get all events on this bus, but never our own, should we happen to accidentally publish some
        hal.subscribeToStatus();
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(LoadRequest.class, req -> {
                    log.info(String.format("Received LoadRequest for order %s", req.getOrderId()));
                    if (currentState.equals(BasicMachineStates.IDLE)) {
                        processLoadRequest(req);
                    } else {
                        String msg = String.format("Received TransportModuleRequest %s in incompatible local state %s", req.getOrderId(), this.currentState);
                        log.warning(msg);
                        getSender().tell(new MachineInWrongStateResponse(machineId.getId(), OPCUABasicMachineBrowsenames.STATE_VAR_NAME, msg, this.currentState, req, BasicMachineStates.IDLE), self());
                    }
                })
                .match(UnloadRequest.class, req -> {
                    log.info(String.format("Received UnloadRequest for order %s", req.getOrderId()));
                })
                .match(MachineStatusUpdateEvent.class, msg -> {
                    processMachineUpdateEvent(msg);
                })
                .match(MachineHistoryRequest.class, req -> {
                    log.info(String.format("Machine %s received MachineHistoryRequest", machineId.getId()));
                    externalHistory.sendHistoryResponseTo(req, getSender(), self());
                })
                .match(Stop.class, req -> {
                    log.info(String.format("TransportModule %s received StopRequest", machineId.getId()));
                    setAndPublishSensedState(BasicMachineStates.STOPPING);
                    hal.stop();
                })
                .match(Reset.class, req -> {
                    if (currentState.equals(BasicMachineStates.COMPLETE)
                            || currentState.equals(BasicMachineStates.STOPPED) ) {
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
                .build();
    }

    private void processLoadRequest(LoadRequest request) {
        setAndPublishSensedState(BasicMachineStates.STARTING);
        orderId = request.getOrderId();
        hal.load(orderId);
    }

    private void processMachineUpdateEvent(MachineStatusUpdateEvent mue) {
        if (mue.getParameterName().equals(OPCUABasicMachineBrowsenames.STATE_VAR_NAME)) {
            BasicMachineStates newState = mue.getStatus();
            setAndPublishSensedState(newState);
            switch (newState) {
                case COMPLETE:
                    orderId = null;
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
                    break;
                case STOPPING:
                    break;
                default:
                    break;
            }
        }

    }

    private void setAndPublishSensedState(BasicMachineStates newState) {
        String order = orderId != null ? orderId : "none";
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
        eventBusByRef.tell(mue, self());
    }

    private void resendLastEvent() {
        context().system()
                .scheduler()
                .scheduleOnce(Duration.ofMillis(1000 * 10),
                        () -> tellEventBusWithoutAddingToHistory(lastMUE),
                        context().system().dispatcher());
    }

}
