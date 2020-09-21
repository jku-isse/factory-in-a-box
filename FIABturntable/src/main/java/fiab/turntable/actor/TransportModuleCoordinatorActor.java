package fiab.turntable.actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.google.common.collect.Sets;

import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.basicmachine.BasicMachineRequests;
import fiab.core.capabilities.basicmachine.events.MachineInWrongStateResponse;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.handshake.HandshakeCapability.ClientMessageTypes;
import fiab.core.capabilities.handshake.HandshakeCapability.ClientSideStates;
import fiab.core.capabilities.handshake.HandshakeCapability.ServerMessageTypes;
import fiab.core.capabilities.handshake.HandshakeCapability.ServerSideStates;
import fiab.core.capabilities.handshake.HandshakeCapability.StateOverrideRequests;
import fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers;
import fiab.handshake.actor.LocalEndpointStatus;
import fiab.turntable.conveying.statemachine.ConveyorStates;
import fiab.turntable.conveying.ConveyorStatusUpdateEvent;
import fiab.turntable.conveying.statemachine.ConveyorTriggers;
import fiab.turntable.opcua.WiringUtils;
import fiab.turntable.turning.TurnRequest;
import fiab.turntable.turning.TurnTableOrientation;
import fiab.turntable.turning.statemachine.TurningStates;
import fiab.turntable.turning.statemachine.TurningTriggers;
import fiab.turntable.turning.TurntableStatusUpdateEvent;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class TransportModuleCoordinatorActor extends AbstractActor {

    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    protected IntraMachineEventBus intraEventBus;
    protected boolean doPublishState = false;
    protected ActorRef self;
    protected BasicMachineStates currentState = BasicMachineStates.STOPPED;
    protected HandshakeEndpointInfo eps;

    protected ActorRef turntableFU;
    protected TurningStates ttFUState = TurningStates.STOPPED;
    protected ActorRef conveyorFU;
    protected ConveyorStates convFUState = ConveyorStates.STOPPED;
    protected InternalProcess exeSubState = InternalProcess.NOPROC;

    protected InternalTransportModuleRequest currentRequest;

    // we need to pass all actors representing server/client handshake and their capability ids
    static public Props props(IntraMachineEventBus internalMachineEventBus, ActorRef turntableFU, ActorRef converyorFU) {
        return Props.create(TransportModuleCoordinatorActor.class, () -> new TransportModuleCoordinatorActor(internalMachineEventBus, turntableFU, converyorFU));
    }

    public TransportModuleCoordinatorActor(IntraMachineEventBus machineEventBus, ActorRef turntableFU, ActorRef converyorFU) {
        this.intraEventBus = machineEventBus;
        self = getSelf();
        intraEventBus.subscribe(self, new SubscriptionClassifier(self.path().name(), "*")); // to obtain events from turntable and conveyor
        eps = new HandshakeEndpointInfo(self);
        this.turntableFU = turntableFU;
        this.conveyorFU = converyorFU;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.class, msg -> {
                    switch (msg) {
                        case Reset:
                            if (currentState.equals(BasicMachineStates.STOPPED) || currentState.equals(BasicMachineStates.COMPLETE))
                                reset();
                            else
                                log.warning("Wrapper told to reset in wrong state " + currentState);
                            break;
                        case Stop:
                            stop();
                            break;
                        case SubscribeState:
                            doPublishState = true;
                            setAndPublishState(currentState);
                            break;
                        default:
                            break;
                    }
                })
                .match(LocalEndpointStatus.LocalClientEndpointStatus.class, les -> {
                    if (!epNonUpdateableStates.contains(currentState)) {
                        this.eps.addOrReplace(les);
                        this.intraEventBus.publish(new WiringUpdateEvent(self.path().name(), les));
                    } else {
                        log.warning("Trying to update Handshake Endpoints in nonupdateable state: " + currentState);
                    }
                })
                .match(LocalEndpointStatus.LocalServerEndpointStatus.class, les -> {
                    if (!epNonUpdateableStates.contains(currentState)) {
                        this.eps.addOrReplace(les);
                    } else {
                        log.warning("Trying to update Handshake Endpoints in nonupdateable state: " + currentState);
                    }
                })
                .match(InternalTransportModuleRequest.class, req -> {
                    if (currentState.equals(BasicMachineStates.IDLE)) {
                        sender().tell(new MachineStatusUpdateEvent(self.path().name(), OPCUABasicMachineBrowsenames.STATE_VAR_NAME, "", BasicMachineStates.STARTING), self);
                        log.info("Received TransportModuleRequest from: " + req.getCapabilityInstanceIdFrom() +
                                ", to: " + req.getCapabilityInstanceIdTo());
                        turnToSource(req);
                    } else {
                        log.warning("Received TransportModuleRequest in incompatible state: " + currentState);
                        //respond with error message that we are not in the right state for request
                        sender().tell(new MachineInWrongStateResponse(getSelf().path().name(),
                                OPCUABasicMachineBrowsenames.STATE_VAR_NAME,
                                "Machine is not in correct state",
                                currentState,
                                req,
                                BasicMachineStates.IDLE), self);
                    }
                })
                .match(ServerSideStates.class, state -> {
                    if (currentState.equals(BasicMachineStates.EXECUTE)) {
                        String capId = getSender().path().name();
                        log.info(String.format("ServerSide EP %s Status: %s", capId, state));
                        String localCapId = capId.lastIndexOf("~") > 0 ? capId.substring(0, capId.lastIndexOf("~")) : capId;

                        eps.getHandshakeEP(localCapId).ifPresent(leps -> {
                            ((LocalEndpointStatus.LocalServerEndpointStatus) leps).setState(state);
                            if (state.equals(ServerSideStates.EXECUTE)) {
                                if (exeSubState.equals(InternalProcess.HANDSHAKE_SOURCE))
                                    startLoadingOntoTurntable();
                                else
                                    startUnloadingFromTurntable();
                            }
//							if (state.equals(ServerSide.COMPLETING)) // superseeded by converyor signal
//								handleCompletingStateUpdate(capId);
                        });
                    } else {
                        log.warning(String.format("Received ServerSide Event %s from %s in non EXECUTE state: %s", state, getSender().path().name(), currentState));
                    }
                })
                .match(ClientSideStates.class, state -> {
                    if (currentState.equals(BasicMachineStates.EXECUTE)) {
                        String capId = getSender().path().name();
                        log.info(String.format("ClientSide EP %s local status: %s", capId, state));
                        String localCapId = capId.lastIndexOf("~") > 0 ? capId.substring(0, capId.lastIndexOf("~")) : capId;

                        eps.getHandshakeEP(localCapId).ifPresent(leps -> {
                            ((LocalEndpointStatus.LocalClientEndpointStatus) leps).setState(state);
                            switch (state) {
                                case EXECUTE:
                                    if (exeSubState.equals(InternalProcess.HANDSHAKE_SOURCE))
                                        startLoadingOntoTurntable();
                                    else if (exeSubState.equals(InternalProcess.HANDSHAKE_DEST))
                                        startUnloadingFromTurntable();
                                    break;
//							case COMPLETING: // superseeded by Conveyor Signal
//								handleCompletingStateUpdate(localCapId);
//								break;
                                case IDLE:
                                    getSender().tell(ClientMessageTypes.Start, self);
                                    break;
                                default:
                                    break;
                            }
                        });
                    } else {
                        log.warning(String.format("Received ClientSide Event %s from %s in non EXECUTE state: %s, ignoring", state, getSender().path().name(), currentState));
                    }
                })
                .match(TurntableStatusUpdateEvent.class, state -> {
                    ttFUState = state.getStatus();
                    switch (state.getStatus()) {
                        case IDLE:
                            if (exeSubState.equals(InternalProcess.TURNING_DEST)) {
                                sendTurntableFuTurnToRequest();
                            }
                            break;
                        case COMPLETING: //fallthrough,
                        case COMPLETE:
                            // signal that handshake can start (if we haven't already done this when COMPLETING was received
                            if (exeSubState.equals(InternalProcess.TURNING_SOURCE)) {
                                startSourceHandshake();
                            } else if (exeSubState.equals(InternalProcess.TURNING_DEST)) {
                                continueWithDestHandshake();
                            }
                            break;
                        case STOPPED:
                        case STOPPING:
                            //TODO: how we handle if FU stops, and we are not in stopping up here
                            break;
                    }
                })
                .match(ConveyorStatusUpdateEvent.class, state -> {
                    convFUState = state.getStatus();
                    if (state.getStatus().equals(ConveyorStates.FULLY_OCCUPIED) && exeSubState.equals(InternalProcess.CONVEYING_SOURCE)) {
                        Optional<LocalEndpointStatus> fromEP = eps.getHandshakeEP(currentRequest.getCapabilityInstanceIdFrom());
                        fromEP.ifPresent(ep -> {
                            if (ep.isProvidedCapability()) {
                                ep.getActor().tell(ServerMessageTypes.Complete, self);
                            } else {
                                ep.getActor().tell(ClientMessageTypes.Complete, self);
                            }
                        });
                        turnToDestination();
                    } else if (state.getStatus().equals(ConveyorStates.IDLE) && exeSubState.equals(InternalProcess.CONVEYING_DEST)) {
                        Optional<LocalEndpointStatus> toEP = eps.getHandshakeEP(currentRequest.getCapabilityInstanceIdTo());
                        toEP.ifPresent(ep -> {
                            if (ep.isProvidedCapability()) {
                                ep.getActor().tell(ServerMessageTypes.Complete, self);
                            } else {
                                ep.getActor().tell(ClientMessageTypes.Complete, self);
                            }
                        });
                        // we reset later anyway, this reset here is too early, moves the turntable away too soon, unloading on receiving side, might not have fully taken over pallet
                        //turntableFU.tell(TurningTriggers.RESET, self); //set the turntable to home position again to counter drifting
                        finalizeTransport();
                    }
                })
                .build();
    }

    private Set<BasicMachineStates> epNonUpdateableStates = Sets.immutableEnumSet(BasicMachineStates.STARTING, BasicMachineStates.EXECUTE, BasicMachineStates.COMPLETING);

    protected void setAndPublishState(BasicMachineStates newState) {
        //log.debug(String.format("%s sets state from %s to %s", this.machineId.getId(), this.currentState, newState));
        this.currentState = newState;
        if (doPublishState) {
            intraEventBus.publish(new MachineStatusUpdateEvent(self.path().name(), OPCUABasicMachineBrowsenames.STATE_VAR_NAME, "", newState));
        }
    }

    private void reset() {
        setAndPublishState(BasicMachineStates.RESETTING);
        turntableFU.tell(TurningTriggers.RESET, self);
        conveyorFU.tell(ConveyorTriggers.RESET, self);
        // EPS are reset upon transport start
        currentRequest = null;
        context().system()
                .scheduler()
                .scheduleOnce(Duration.ofMillis(500), // we wait in transitioncheck (see below) from resetting to idle anyway of turntable and conveyor are in state idle
                        new Runnable() {
                            @Override
                            public void run() {
                                transitionResettingToIdle();
                            }
                        }, context().system().dispatcher());
    }

    private void transitionResettingToIdle() {
        context().system()
                .scheduler()
                .scheduleOnce(Duration.ofMillis(500),
                        new Runnable() {
                            @Override
                            public void run() {
                                if (ttFUState.equals(TurningStates.IDLE) && convFUState.equals(ConveyorStates.IDLE)) {
                                    setAndPublishState(BasicMachineStates.IDLE); // only if FUs are also resetted to Idle can we also go into Idle
                                } else {
                                    log.info("Waiting for FUs to turn IDLE before becoming IDLE oneself");
                                    transitionResettingToIdle();
                                }
                            }
                        }, context().system().dispatcher());
    }

    private void turnToSource(InternalTransportModuleRequest req) {
        log.info("Starting Transport");
        currentRequest = req;
        setAndPublishState(BasicMachineStates.STARTING);
        // check which two handshake FUs we use,
        Optional<LocalEndpointStatus> fromEP = eps.getHandshakeEP(req.getCapabilityInstanceIdFrom());
        Optional<LocalEndpointStatus> toEP = eps.getHandshakeEP(req.getCapabilityInstanceIdTo());
        if (fromEP.isPresent() && toEP.isPresent()) {
            setAndPublishState(BasicMachineStates.EXECUTE);
            turntableFU.tell(new TurnRequest(resolveCapabilityToOrientation(fromEP.get())), self);
            setExeSubState(InternalProcess.TURNING_SOURCE);
        } else {
            log.warning("A HandshakeEndpoint could not be identified! From: " + fromEP.isPresent() + ", To: " + toEP.isPresent());
            if (!fromEP.isPresent())
                log.warning("Unknown HandshakeEndpoint identified by CapabilityId " + req.getCapabilityInstanceIdFrom());
            if (!toEP.isPresent())
                log.warning("Unknown HandshakeEndpoint identified by CapabilityId " + req.getCapabilityInstanceIdTo());
            stop();
        }
    }

    private TurnTableOrientation resolveCapabilityToOrientation(LocalEndpointStatus les) {
        if (les.getCapabilityId().startsWith("NORTH"))
            return TurnTableOrientation.NORTH;
        else if (les.getCapabilityId().startsWith("SOUTH"))
            return TurnTableOrientation.SOUTH;
        else if (les.getCapabilityId().startsWith("WEST"))
            return TurnTableOrientation.WEST;
        else if (les.getCapabilityId().startsWith("EAST"))
            return TurnTableOrientation.EAST;
        else
            log.error("Cannot resolve CapabilityId to TurntableOrientation: " + les.getCapabilityId());
        return null; //TODO: better handling than via null needed
    }

    private void startSourceHandshake() {
        Optional<LocalEndpointStatus> fromEP = eps.getHandshakeEP(currentRequest.getCapabilityInstanceIdFrom());
        Optional<LocalEndpointStatus> toEP = eps.getHandshakeEP(currentRequest.getCapabilityInstanceIdTo());
        log.info("Trying to start handshake");
        if (fromEP.isPresent() && toEP.isPresent()) {
            fromEP.ifPresent(leps -> {
                setExeSubState(InternalProcess.HANDSHAKE_SOURCE);
                // now check if localEP is client or server, then reset
                if (leps.isProvidedCapability()) {
                    leps.getActor().tell(ServerMessageTypes.Reset, self);
                } else {
                    leps.getActor().tell(ClientMessageTypes.Reset, self);
                }
            });
        }
    }

    private void startLoadingOntoTurntable() {
        log.info("Starting to load turntable");
        setExeSubState(InternalProcess.CONVEYING_SOURCE);
        conveyorFU.tell(ConveyorTriggers.LOAD, self);
    }

//	private void handleCompletingStateUpdate(String capId) {
//		try {
//		if (currentRequest.getCapabilityInstanceIdFrom().equals(capId)) { // first finished
//			continueWithDestHandshake();
//		} else if (currentRequest.getCapabilityInstanceIdTo().equals(capId)) { //second finished
//			finalizeTransport();
//		}
//		} catch(Exception e) {
//			// woops
//			System.out.println(capId);
//		}
//	}

    private void turnToDestination() {
        turntableFU.tell(TurningTriggers.RESET, self); //set the turntable to home position again to counter drifting
        log.info("Starting to Turn to Destination");
        Optional<LocalEndpointStatus> toEP = eps.getHandshakeEP(currentRequest.getCapabilityInstanceIdTo());
        toEP.ifPresent(ep -> setExeSubState(InternalProcess.TURNING_DEST));
        /*toEP.ifPresent(ep -> {
            context().system()
                    .scheduler()
                    .scheduleOnce(Duration.ofMillis(5000), //waiting to turntable having reached home position, very ugly this way
                            new Runnable() {
                                @Override
                                public void run() {
                                    turntableFU.tell(new TurnRequest(resolveCapabilityToOrientation(ep)), self);
                                    setExeSubState(InternalProcess.TURNING_DEST);
                                }
                            }, context().system().dispatcher());
        });*/
    }

    private void sendTurntableFuTurnToRequest() {
        context().system()
                .scheduler()
                .scheduleOnce(Duration.ofMillis(5000), () -> {
                    Optional<LocalEndpointStatus> toEP = eps.getHandshakeEP(currentRequest.getCapabilityInstanceIdTo());
                    toEP.ifPresent(ep -> turntableFU.tell(new TurnRequest(resolveCapabilityToOrientation(ep)), self));
                }, context().system().dispatcher());
    }

    private void continueWithDestHandshake() {
        log.info("Continuing with Destination Handshake via: " + currentRequest.getCapabilityInstanceIdTo());
        // imitate turning towards second
        Optional<LocalEndpointStatus> toEP = eps.getHandshakeEP(currentRequest.getCapabilityInstanceIdTo());
        // now check if localEP is client or server
        // reset the second,
        toEP.ifPresent(leps -> {
            // now check if localEP is client or server, then reset
            setExeSubState(InternalProcess.HANDSHAKE_DEST);
            if (leps.isProvidedCapability()) {
                // as the second transport part, the this server/turntable has to be loaded
                leps.getActor().tell(StateOverrideRequests.SetLoaded, self);
                leps.getActor().tell(ServerMessageTypes.Reset, self);
            } else {
                leps.getActor().tell(ClientMessageTypes.Reset, self);
            }
        });
        // when execute, immitate loading, complete second
    }

    private void startUnloadingFromTurntable() {
        log.info("Starting to unload turntable");
        conveyorFU.tell(ConveyorTriggers.UNLOAD, self);
        setExeSubState(InternalProcess.CONVEYING_DEST);
    }

    private void finalizeTransport() {
        log.info("Finalizing Transport");
        // transition into Completing, handshakes should be now in complete as well
        setAndPublishState(BasicMachineStates.COMPLETING);
        context().system()
                .scheduler()
                .scheduleOnce(Duration.ofMillis(2000), // time available for receiving side to fully take over pallet
                        new Runnable() {
                            @Override
                            public void run() {
                                setAndPublishState(BasicMachineStates.COMPLETE);
                                //we do autoresetting here
                                reset();
                            }
                        }, context().system().dispatcher());
    }


    private void stop() {
        setAndPublishState(BasicMachineStates.STOPPING);
        setExeSubState(InternalProcess.NOPROC);
        //tell all handshake FUs to stop, we ignore HandshakeFU level for now
        eps.tellAllEPsToStop();
        // serverSide.tell(MockServerHandshakeActor.MessageTypes.Stop, getSelf());
        turntableFU.tell(TurningTriggers.STOP, self);
        conveyorFU.tell(ConveyorTriggers.STOP, self);
        context().system()
                .scheduler()
                .scheduleOnce(Duration.ofMillis(1000),
                        new Runnable() {
                            @Override
                            public void run() {
                                // only when handshakeFU and other FUs have stopped
                                //if (handshakeStatus.equals(ServerSide.Stopped)) {
                                transitionToStop();
                                //}
                            }
                        }, context().system().dispatcher());
    }

    private void transitionToStop() {
        setAndPublishState(BasicMachineStates.STOPPED);
    }

    private void setExeSubState(InternalProcess process) {
        exeSubState = process;
        log.info("Setting exeSubstate to " + process.toString());
    }

    private static enum InternalProcess {
        TURNING_SOURCE, HANDSHAKE_SOURCE, CONVEYING_SOURCE, TURNING_DEST, HANDSHAKE_DEST, CONVEYING_DEST, NOPROC
    }

    private static class HandshakeEndpointInfo {
        protected Map<String, LocalEndpointStatus> handshakeEPs = new HashMap<>();

        ActorRef self;

        protected HandshakeEndpointInfo(ActorRef self) {
            this.self = self;
        }

//		public HandshakeEndpointInfo(Map<String,LocalEndpointStatus> handshakeEPs) {
//			this.handshakeEPs = handshakeEPs;
//		}

        public Optional<LocalEndpointStatus> getHandshakeEP(String capabilityId) {
            if (capabilityId != null && handshakeEPs.containsKey(capabilityId))
                return Optional.ofNullable(handshakeEPs.get(capabilityId));
            else
                return Optional.empty();
        }

        public void addOrReplace(LocalEndpointStatus les) {
            handshakeEPs.put(les.getCapabilityId(), les);
        }

        public void tellAllEPsToStop() {
            handshakeEPs.values().stream()
                    .forEach(les -> {
                        if (les.isProvidedCapability()) {                    // if server use server msg
                            les.getActor().tell(ServerMessageTypes.Stop, self);
                        } else {
                            les.getActor().tell(ClientMessageTypes.Stop, self);
                        }
                    });
        }

    }


}
