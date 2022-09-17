package fiab.mes.machine.actor.iostation;

import ActorCoreModel.Actor;
import ProcessCore.AbstractCapability;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.core.capabilities.handshake.IOStationCapability;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.functionalunit.connector.MachineEventBus;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.general.HistoryTracker;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.actor.iostation.wrapper.IOStationWrapperInterface;
import fiab.mes.machine.msg.GenericMachineRequests.Reset;
import fiab.mes.machine.msg.GenericMachineRequests.Stop;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.machine.msg.MachineDisconnectedEvent;
import fiab.mes.order.msg.CancelOrTerminateOrder;
import fiab.mes.order.msg.LockForOrder;
import fiab.mes.order.msg.ReadyForProcessEvent;
import fiab.mes.order.msg.RegisterProcessStepRequest;
import fiab.mes.restendpoint.requests.MachineHistoryRequest;

import java.time.Duration;
import java.util.*;

public class BasicIOStationActor extends AbstractActor {

    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    protected ActorSelection eventBusByRef;
    protected final AkkaActorBackedCoreModelAbstractActor machineId;
    protected AbstractCapability cap;
    protected ServerSideStates currentState = ServerSideStates.UNKNOWN;
    protected IOStationWrapperInterface hal;
    protected MachineEventBus intraBus;
    protected boolean doAutoReset = true;
    protected boolean isInputStation = false;
    protected boolean isOutputStation = false;
    private ActorRef self;
    protected HistoryTracker externalHistory = null;

    protected List<RegisterProcessStepRequest> orders = new ArrayList<>();
    private String lastOrder;
    protected RegisterProcessStepRequest reservedForOrder = null;

    //These state updates are published before the actual machine sets it's state
    private Set<ServerSideStates> aheadOfTimeStateUpdates = Set.of(ServerSideStates.RESETTING, ServerSideStates.STOPPING);

    static public Props props(ActorSelection machineEventBus, AbstractCapability cap, Actor modelActor, IOStationWrapperInterface hal, MachineEventBus intraBus) {
        return Props.create(BasicIOStationActor.class, () -> new BasicIOStationActor(machineEventBus, cap, modelActor, hal, intraBus));
    }

    public BasicIOStationActor(ActorSelection machineEventBus, AbstractCapability cap, Actor modelActor, IOStationWrapperInterface hal, MachineEventBus intraBus) {
        this.cap = cap;
        this.machineId = new AkkaActorBackedCoreModelAbstractActor(modelActor.getID(), modelActor, self());
        this.eventBusByRef = machineEventBus;
        this.hal = hal;
        this.intraBus = intraBus;
        this.externalHistory = new HistoryTracker(machineId.getId());
        this.self = self();
        init();
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(RegisterProcessStepRequest.class, registerReq -> {
                    orders.add(registerReq);
                    log.info(String.format("Order %s registered.", registerReq.getRootOrderId()));
                    if ((currentState == ServerSideStates.IDLE_EMPTY && isOutputStation) ||
                            (currentState == ServerSideStates.IDLE_LOADED && isInputStation)) {
                        triggerNextQueuedOrder();
                    }
                })
                .match(LockForOrder.class, lockReq -> {
                    log.info("received LockForOrder msg " + lockReq.getStepId() + ", current state: " + currentState);
                    if ((currentState == ServerSideStates.IDLE_EMPTY && isOutputStation) ||
                            (currentState == ServerSideStates.IDLE_LOADED && isInputStation)) {
                        // we are still in the right state, now we provide/receive the reserved order
                        // nothing to be done here
                    } else {
                        log.warning("Received lock for order in state: " + currentState);
                    }
                })
                .match(CancelOrTerminateOrder.class, cto -> {
                    log.info(String.format("IOStation %s received CancelOrTerminateOrder request for %s", machineId.getId(), cto.getRootOrderId()));
                    handleOrderCancelRequest(cto);
                })
                .match(IOStationStatusUpdateEvent.class, mue -> {
                    processIOStationStatusUpdateEvent(mue);
                })
                .match(Stop.class, req -> {
                    log.info(String.format("IOStation %s received StopRequest", machineId.getId()));
                    setAndPublishSensedState(ServerSideStates.STOPPING);
                    hal.stop();
                })
                .match(Reset.class, req -> {
                    if (currentState.equals(ServerSideStates.COMPLETE)
                            || currentState.equals(ServerSideStates.STOPPED)) {
                        log.info(String.format("IOStation %s received ResetRequest in suitable state", machineId.getId()));
                        setAndPublishSensedState(ServerSideStates.RESETTING); // not sensed, but machine would do the same (or fail, then we need to wait for machine to respond)
                        hal.reset();
                    } else {
                        log.warning(String.format("IOStation %s received ResetRequest in non-COMPLETE or non-STOPPED state, ignoring", machineId.getId()));
                    }
                })
                .match(MachineHistoryRequest.class, req -> {
                    log.info(String.format("Machine %s received MachineHistoryRequest", machineId.getId()));
                    externalHistory.sendHistoryResponseTo(req, getSender(), self);
                })
                .match(MachineDisconnectedEvent.class, req -> {
                    log.warning(String.format("Lost connection to machine in state: %s, sending disconnected event and shutting down actor", this.currentState));
                    eventBusByRef.tell(new MachineDisconnectedEvent(machineId), self());
                })
                .build();
    }


    private void init() {
        if (this.cap.equals(IOStationCapability.getInputStationCapability())) {
            isInputStation = true;
        }
        if (this.cap.equals(IOStationCapability.getOutputStationCapability())) {
            isOutputStation = true;
        }
        eventBusByRef.tell(new MachineConnectedEvent(machineId, Collections.singleton(cap), Collections.emptySet()), self());
        intraBus.subscribe(getSelf(), new MESSubscriptionClassifier(machineId.getId(), "*")); //ensure we get all events on this bus, but never our own, should we happen to accidentally publish some
        hal.subscribeToStatus();
        hal.subscribeToLoadStatus();
    }

    private void setAndPublishSensedState(ServerSideStates newState) {
        String msg = String.format("%s sets state from %s to %s (Order: %s)", this.machineId.getId(), this.currentState, newState, lastOrder);
        log.info(msg);
        this.currentState = newState;
        IOStationStatusUpdateEvent mue = new IOStationStatusUpdateEvent(machineId.getId(), msg, newState);
        externalHistory.add(mue);
        tellEventBus(mue);
    }

    private void tellEventBus(IOStationStatusUpdateEvent mue) {
        //externalHistory.add(mue);
        eventBusByRef.tell(mue, self());
    }

    private void processIOStationStatusUpdateEvent(IOStationStatusUpdateEvent mue) {
        if (mue.getParameterName().equals(IOStationCapability.OPCUA_STATE_SERVERSIDE_VAR_NAME)) {
            ServerSideStates newState = mue.getStatus();
            if (!aheadOfTimeStateUpdates.contains(newState)) {
                setAndPublishSensedState(newState);    //Avoid sending duplicate events
            }
            switch (newState) {
                case IDLE_EMPTY:
                    if (isOutputStation) { // ready to receive pallet as an outputstation
                        triggerNextQueuedOrder();
                    }
                    break;
                case IDLE_LOADED:
                    if (isInputStation) { // ready to provide pallet as an inputstation
                        triggerNextQueuedOrder();
                    }
                    break;
                case STOPPED:
                    if (doAutoReset)
                        reset();
                    break;
                case COMPLETE:
                    reset(); //we automatically reset, might be done also by station itself, but we need to clean state here as well
                    break;
                default:
                    break;

            }
        }
    }

    private void handleOrderCancelRequest(CancelOrTerminateOrder cto) {
        // if in orders, just remove,
        Optional<RegisterProcessStepRequest> req = orders.stream()
                .filter(rpsr -> rpsr.getRootOrderId().equals(cto.getRootOrderId()))
                .findAny();
        req.ifPresent(r -> {
            orders.remove(r);
        });
        // if current order
        if (reservedForOrder != null && reservedForOrder.getRootOrderId().equals(cto.getRootOrderId())) {
            switch (currentState) {
                case IDLE_EMPTY: //falltrough
                case IDLE_LOADED: // cancel by stopping and autoresetting
                    //setAndPublishSensedState(ServerSideStates.STOPPING);
                    hal.stop();
                    break;
                default: // finish handshake, or just remain in whatever state otherwise, stopping, etc,
            }

        }
    }


    private void triggerNextQueuedOrder() {
        if (!orders.isEmpty() && reservedForOrder == null) {
            RegisterProcessStepRequest ror = orders.remove(0);
            lastOrder = ror.getRootOrderId();
            log.info("Ready for next Order: " + ror.getRootOrderId());
            reservedForOrder = ror;
            ror.getRequestor().tell(new ReadyForProcessEvent(ror), getSelf());
        }
    }

    private void reset() {
        reservedForOrder = null;
        lastOrder = null;
        hal.reset();
    }

    //Not necessary since we already do autoReset
    /*private void scheduleAutoReset() {
        context().system().scheduler().scheduleOnce(Duration.ofSeconds(3), () -> {
            if (currentState == ServerSideStates.STOPPED || currentState == ServerSideStates.COMPLETE) {
                //We are in a state where we can reset
                reset();
            }else if(currentState == ServerSideStates.STOPPING || currentState == ServerSideStates.COMPLETING){
                //Here we wait until we reach a state from where we can reset
                scheduleAutoReset();
            }
        }, context().dispatcher());
    }*/

}
