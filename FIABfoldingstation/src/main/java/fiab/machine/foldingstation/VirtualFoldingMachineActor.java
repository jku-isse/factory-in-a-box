package fiab.machine.foldingstation;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.basicmachine.events.MachineInWrongStateResponse;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.folding.FoldingMessageTypes;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.core.capabilities.handshake.IOStationCapability;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.handshake.actor.LocalEndpointStatus;
import fiab.handshake.actor.ServerSideHandshakeActor;
import fiab.machine.foldingstation.events.MachineCapabilityUpdateEvent;

import java.time.Duration;

public class VirtualFoldingMachineActor extends AbstractActor {
    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private String machineName = "VirtualFoldingStation";
    protected IntraMachineEventBus intraEventBus;
    protected BasicMachineStates currentState = BasicMachineStates.STOPPING;
    protected boolean doPublishState = false;
    protected ServerSideStates handshakeStatus;
    protected ActorRef serverSide;
    protected ActorRef self;

    static public Props propsForLateHandshakeBinding(IntraMachineEventBus internalMachineEventBus) {
        return Props.create(VirtualFoldingMachineActor.class, () -> new VirtualFoldingMachineActor(internalMachineEventBus, true));
    }

    static public Props props(IntraMachineEventBus internalMachineEventBus) {
        return Props.create(VirtualFoldingMachineActor.class, () -> new VirtualFoldingMachineActor(internalMachineEventBus, false));
    }

    public VirtualFoldingMachineActor(IntraMachineEventBus machineEventBus, boolean doLateBinding) {
        this.intraEventBus = machineEventBus;
        // setup serverhandshake actor with autocomplete

        self = getSelf();
        //serverSide = getContext().actorOf(MockServerHandshakeActor.props(getSelf(), doAutoComplete).withDispatcher(CallingThreadDispatcher.Id()), "ServerSideHandshakeMock");
        if (!doLateBinding) {
            boolean doAutoComplete = true;
            serverSide = getContext().actorOf(ServerSideHandshakeActor.props(getSelf(), doAutoComplete), "ServerSideHandshakeMock");
            this.setAndPublishState(BasicMachineStates.STOPPED);
        }
    }

    public Receive createReceive() {
        return receiveBuilder()
                .match(FoldingMessageTypes.class, msg -> {
                    switch (msg) {
                        case SubscribeState:
                            doPublishState = true;
                            setAndPublishState(currentState); //we publish the current state
                            break;
                        case Fold:
                            if (currentState.equals(BasicMachineStates.IDLE)) {
                                fold();
                            } else {
                                log.warning("FoldingCoordinatorActor told to fold in wrong state " + currentState);
                                sender().tell(new MachineInWrongStateResponse(machineName, OPCUABasicMachineBrowsenames.STATE_VAR_NAME, "Machine not in state to fold", currentState, FoldingMessageTypes.Fold, BasicMachineStates.IDLE), self);
                            }
                            break;
                        case Reset:
                            if (currentState.equals(BasicMachineStates.STOPPED))
                                reset();
                            else
                                log.warning("Wrapper told to reset in wrong state " + currentState);
                            break;
                        case Stop:
                            stop();
                            break;
                        case SetCapability:
                            updateCapability("Updated");
                            break;
                        default:
                            break;
                    }
                })
                .match(ServerSideStates.class, msg -> { // state event updates
                    log.info(String.format("Received %s from %s", msg, getSender()));
                    //if (getSender().equals(serverSide)) {
                    handshakeStatus = msg;
                    switch (msg) {
                        case COMPLETE: // handshake complete, thus un/loading done
                            if (currentState.equals(BasicMachineStates.STARTING)) { // pallet is now loaded
                                transitionStartingToExecute();
                            }
                            break;
                        case STOPPED:
                            if (currentState.equals(BasicMachineStates.STOPPING)) { //only if we wait for FU to stop, alternative way to learn about serverside
                                if (serverSide == null) {
                                    setServerHandshakeActor(getSender()); // will also result in transition to stop
                                } else
                                    transitionToStop();
                            }
                            break;
                        default: // irrelevant states
                            break;
                    }
                    //} else {
                    //	log.warning(String.format("Received %s from unexpected sender %s", msg, getSender()));
                    //}
                })
                .match(ActorRef.class, lateBoundHandshake -> {
                    setServerHandshakeActor(lateBoundHandshake); //wont be called when serverhandshake announces itself to its parentActor, and parentActor is set to this actor
                })
                .match(LocalEndpointStatus.LocalServerEndpointStatus.class, les -> {
                    //setServerHandshakeActor(les.getActor()); //FIXME
                })
                .matchAny(msg -> {
                    log.warning("Unexpected Message received: " + msg.toString());
                })
                .build();
    }

    private void setServerHandshakeActor(ActorRef serverSide) {
        if (this.currentState.equals(BasicMachineStates.STOPPING)) {
            this.serverSide = serverSide;
            setAndPublishState(BasicMachineStates.STOPPED);
        }
    }

    private void updateCapability(String newCapability) {
        intraEventBus.publish(new MachineCapabilityUpdateEvent(machineName, "Fold_Capability", newCapability));
    }

    private void setAndPublishState(BasicMachineStates newState) {
        //log.debug(String.format("%s sets state from %s to %s", this.machineId.getId(), this.currentState, newState));
        this.currentState = newState;
        if (doPublishState) {
            intraEventBus.publish(new MachineStatusUpdateEvent(machineName, OPCUABasicMachineBrowsenames.STATE_VAR_NAME, "Status update", newState));
        }
    }

    private void reset() {
        setAndPublishState(BasicMachineStates.RESETTING);
        context().system()
                .scheduler()
                .scheduleOnce(Duration.ofMillis(1000),
                        new Runnable() {
                            @Override
                            public void run() {
                                // only when foldingFU is in idle, but we dont care about transport/handshake FU, which stays in stopped as the order for which we load is not clear yet
                                setAndPublishState(BasicMachineStates.IDLE);
                            }
                        }, context().system().dispatcher());
    }

    private void stop() {
        setAndPublishState(BasicMachineStates.STOPPING);
        serverSide.tell(IOStationCapability.ServerMessageTypes.Stop, getSelf());
        context().system()
                .scheduler()
                .scheduleOnce(Duration.ofMillis(1000),
                        new Runnable() {
                            @Override
                            public void run() {
                                // only when handshakeFU and other FUs have stopped
                                if (handshakeStatus.equals(ServerSideStates.STOPPED)) {
                                    transitionToStop();
                                }
                            }
                        }, context().system().dispatcher());
    }

    private void transitionToStop() {
        setAndPublishState(BasicMachineStates.STOPPED);
    }

    private void fold() {
        setAndPublishState(BasicMachineStates.STARTING);
        sender().tell(new MachineStatusUpdateEvent("", OPCUABasicMachineBrowsenames.STATE_VAR_NAME, "", currentState), self);
        //now here we also enable pallet to be loaded onto machine
        serverSide.tell(HandshakeCapability.ServerMessageTypes.Reset, self);
        context().system()
                .scheduler()
                .scheduleOnce(Duration.ofMillis(3000),
                        new Runnable() {
                            @Override
                            public void run() {
                                // we only transition when the pallet is loaded, e.g., the server handshake is completing or completed,
                                //sending of the complete() command (by the here nonexisting converyerFU when loaded) --> not necessary if we set serverside protocol actor to auto-complete
                                if (handshakeStatus.equals(ServerSideStates.COMPLETE)) {
                                    transitionStartingToExecute();
                                }
                            }
                        }, context().system().dispatcher());
    }

    private void transitionStartingToExecute() {
        setAndPublishState(BasicMachineStates.EXECUTE);
        //TODO check input from touch sensor to continue
        /*context().system()
                .scheduler()
                .scheduleOnce(Duration.ofSeconds(5),
                        new Runnable() {
                            @Override
                            public void run() {
                                finishProduction();
                            }
                        }, context().system().dispatcher());*/
    }

    private void finishProduction() {
        setAndPublishState(BasicMachineStates.COMPLETING);
        /*serverSide.tell(HandshakeCapability.ServerMessageTypes.Reset, self); //now again do a handshake and unload,
        context().system()
                .scheduler()
                .scheduleOnce(Duration.ofMillis(3000),
                        new Runnable() {
                            @Override
                            public void run() {
                                if (handshakeStatus.equals(HandshakeCapability.ServerSideStates.COMPLETE)) {
                                    //only when the handshake is in completed are we good to continue, actually we only care about loadstate
                                    transitionCompletingToComplete();
                                }
                            }
                        }, context().system().dispatcher());*/
        context().system()
                .scheduler()
                .scheduleOnce(Duration.ofMillis(2000), this::transitionCompletingToComplete, context().dispatcher());
    }

    private void transitionCompletingToComplete() {
        setAndPublishState(BasicMachineStates.COMPLETE);
        context().system() // we automatically reset
                .scheduler()
                .scheduleOnce(Duration.ofMillis(2000), this::reset, context().dispatcher());

    }

}
