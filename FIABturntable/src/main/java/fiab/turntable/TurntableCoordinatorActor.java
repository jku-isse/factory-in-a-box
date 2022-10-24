package fiab.turntable;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.github.oxo42.stateless4j.StateMachine;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.StatePublisher;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.functionalunit.ResetRequest;
import fiab.core.capabilities.functionalunit.StopRequest;
import fiab.core.capabilities.handshake.ClientSideStates;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.core.capabilities.handshake.client.CompleteHandshake;
import fiab.core.capabilities.handshake.client.PerformHandshake;
import fiab.core.capabilities.transport.TransportDestinations;
import fiab.core.capabilities.transport.TransportModuleCapability;
import fiab.core.capabilities.transport.TransportRequest;
import fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers;
import fiab.functionalunit.connector.FUConnector;
import fiab.functionalunit.connector.FUSubscriptionClassifier;
import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.functionalunit.connector.MachineEventBus;
import fiab.handshake.client.messages.ClientHandshakeStatusUpdateEvent;
import fiab.handshake.client.messages.WiringRequest;
import fiab.handshake.client.messages.WiringUpdateNotification;
import fiab.handshake.server.messages.ServerHandshakeStatusUpdateEvent;
import fiab.conveyor.ConveyorCapability;
import fiab.conveyor.messages.ConveyorStatusUpdateEvent;
import fiab.conveyor.messages.LoadConveyorRequest;
import fiab.conveyor.messages.UnloadConveyorRequest;
import fiab.functionalunit.MachineChildFUs;
import fiab.turntable.statemachine.process.ProcessStateMachine;
import fiab.turntable.statemachine.process.ProcessStates;
import fiab.turntable.statemachine.process.ProcessTriggers;
import fiab.turntable.statemachine.turntable.TurntableStateMachine;
import fiab.turntable.statemachine.turntable.TurntableTriggers;
import fiab.turntable.turning.TurningCapability;
import fiab.turntable.turning.messages.TurnRequest;
import fiab.turntable.turning.messages.TurningStatusUpdateEvent;
import fiab.turntable.turning.statemachine.TurningStates;


public class TurntableCoordinatorActor extends AbstractActor implements TransportModuleCapability, StatePublisher {

    public static Props props(MachineEventBus machineEventBus, IntraMachineEventBus intraMachineEventBus, MachineChildFUs infrastructure) {
        return Props.create(TurntableCoordinatorActor.class, () -> new TurntableCoordinatorActor(machineEventBus, intraMachineEventBus, infrastructure));
    }

    protected final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final String componentId;
    protected MachineEventBus machineEventBus;
    protected IntraMachineEventBus intraMachineBus;
    protected StateMachine<BasicMachineStates, Object> stateMachine;
    protected StateMachine<ProcessStates, Object> process;
    protected TransportRequest currentRequest;

    //Track these in separate class
    protected FUStateInfo fuStateInfo;
    protected MachineChildFUs machineChildFUs;

    public TurntableCoordinatorActor(MachineEventBus machineEventBus, IntraMachineEventBus intraMachineEventBus, MachineChildFUs infrastructure) {
        this.componentId = self().path().name();
        this.fuStateInfo = new FUStateInfo(self());
        this.machineEventBus = machineEventBus;
        this.intraMachineBus = intraMachineEventBus;
        this.stateMachine = new TurntableStateMachine(fuStateInfo);
        this.process = new ProcessStateMachine(fuStateInfo);
        this.machineChildFUs = infrastructure;
        init();
    }

    private void init() {
        subscribeToConnectors();
        addActionsToStates();
        addActionsToProcessSteps();
        publishCurrentState(this.stateMachine.getState());
        machineChildFUs.setupInfrastructure(context(), intraMachineBus);
    }

    private void subscribeToConnectors() {
        FUSubscriptionClassifier FUSubscriptionClassifier = new FUSubscriptionClassifier(this.componentId, "*");
        this.intraMachineBus.subscribe(self(), FUSubscriptionClassifier);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.class, req -> {
                    //This handler should be removed and ResetReq/StopReq should be used instead
                    handleLegacyStopResetRequests(req);
                })
                .match(TransportRequest.class, req -> {
                    transport(req);
                })
                .match(TurningStatusUpdateEvent.class, msg -> {
                    handleTurningStatusUpdate(msg);
                })
                .match(ConveyorStatusUpdateEvent.class, msg -> {
                    fuStateInfo.setConveyorFuState(msg.getStatus());
                })
                .match(ServerHandshakeStatusUpdateEvent.class, msg -> {
                    handleServerStatusUpdate(msg);
                })
                .match(ClientHandshakeStatusUpdateEvent.class, msg -> {
                    handleClientStatusUpdate(msg);
                })
                .match(WiringRequest.class, msg -> {
                    handleWiringRequest(msg);
                })
                .match(WiringUpdateNotification.class, msg ->{
                    //TODO, maybe just reuse WiringRequest?
                })
                .build();
    }

    private void addActionsToStates() {
        for (BasicMachineStates state : BasicMachineStates.values()) {
            this.stateMachine.configure(state).onEntry(() -> publishCurrentState(state));
        }
        this.stateMachine.configure(BasicMachineStates.STOPPING).onEntry(this::doStopping);
        this.stateMachine.configure(BasicMachineStates.RESETTING).onEntry(this::doResetting);
        this.stateMachine.configure(BasicMachineStates.EXECUTE).onEntry(this::activateTransportProcess);
    }

    private void addActionsToProcessSteps() {
        for (ProcessStates processStep : ProcessStates.values()) {
            this.process.configure(processStep).onEntry(() -> publishCurrentProcessStep(processStep.name()));
        }
        this.process.configure(ProcessStates.TURNING_SOURCE).onEntry(this::turnToSource);
        this.process.configure(ProcessStates.HANDSHAKE_SOURCE).onEntry(this::prepareHandshakeSource);
        this.process.configure(ProcessStates.CONVEYING_SOURCE).onEntry(this::conveyPalletSource)
                .onExit(this::completeHandshakeSource);
        this.process.configure(ProcessStates.TURNING_DEST).onEntry(this::resetTurningForDestination);
        this.process.configure(ProcessStates.HANDSHAKE_DEST).onEntry(this::prepareHandshakeDestination);
        this.process.configure(ProcessStates.CONVEYING_DEST).onEntry(this::conveyPalletDestination)
                .onExit(this::completeHandshakeDestination);
        this.process.configure(ProcessStates.DONE).onEntry(this::doCompleting);
    }

    @Override
    public void doResetting() {
        fireProcessStepTransition(ProcessTriggers.CLEAR_PROCESS);
        FUConnector turningConnector = getConnectorForId(TurningCapability.CAPABILITY_ID);
        FUConnector conveyorConnector = getConnectorForId(ConveyorCapability.CAPABILITY_ID);
        if (turningConnector == null || conveyorConnector == null) {
            if (turningConnector == null) {
                log.warning("Could not find turning component. Please make sure you instantiated them properly");
            }
            if (conveyorConnector == null) {
                log.warning("Could not find conveyor component. Please make sure you instantiated them properly");
            }
        } else {
            getConnectorForId(TurningCapability.CAPABILITY_ID).publish(new ResetRequest(this.componentId));
            getConnectorForId(ConveyorCapability.CAPABILITY_ID).publish(new ResetRequest(this.componentId));
        }
    }

    @Override
    public void doStopping() {
        for (FUConnector connector : machineChildFUs.getFuConnectors().values()) {
            connector.publish(new StopRequest(this.componentId));
        }
        cleanupProcess();
    }

    @Override
    public void transport(TransportRequest req) {
        fireIfPossible(TurntableTriggers.START);
        this.currentRequest = req;
        boolean isHsSourcePresent = fuStateInfo.getHandshakeEndpointInfo().getHandshakeForCapId(req.getCapabilityInstanceIdFrom()).isPresent();
        boolean isHsDestinationPresent = fuStateInfo.getHandshakeEndpointInfo().getHandshakeForCapId(req.getCapabilityInstanceIdTo()).isPresent();
        if (isHsSourcePresent && isHsDestinationPresent) {
            fireIfPossible(TurntableTriggers.EXECUTE);
        } else {
            if (!isHsSourcePresent)
                log.warning("Could not find suitable handshake for capId " + req.getCapabilityInstanceIdFrom());
            if (!isHsDestinationPresent)
                log.warning("Could not find suitable handshake for capId " + req.getCapabilityInstanceIdTo());
            fireIfPossible(TurntableTriggers.STOP);
        }
    }

    protected void handleLegacyStopResetRequests(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes req) {
        if (req == TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.Reset) {
            fireIfPossible(TurntableTriggers.RESET);
        } else if (req == TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.Stop) {
            fireIfPossible(TurntableTriggers.STOP);
        }
    }

    protected void handleWiringRequest(WiringRequest msg) {
        log.info("Applying wiringInfo for local hs cap: {}, to endpoint at: {}",
                msg.getInfo().getLocalCapabilityId(), msg.getInfo().getRemoteEndpointURL());
        FUConnector handshakeConn;
        //While each handshake has the local capability e.g. NORTH_CLIENT, it is necessary
        //to add HANDSHAKE_FU_ as a prefix as the handshake actor has this id
        String handshakeConnectorId = msg.getInfo().getLocalCapabilityId();
        handshakeConn = machineChildFUs.getFUConnectorForCapabilityId(handshakeConnectorId);
        if (handshakeConn != null) {
            handshakeConn.publish(msg);
        } else {
            log.warning("Could not find compatible eventBus to publish WiringRequest for local capId="
                    + msg.getInfo().getLocalCapabilityId());
        }
    }

    protected void handleTurningStatusUpdate(TurningStatusUpdateEvent msg) {
        log.info("TurningFU sent update " + msg.getStatus());
        fuStateInfo.setTurningFuState(msg.getStatus());
        if (msg.getStatus().equals(TurningStates.IDLE)) {
            if (process.isInState(ProcessStates.TURNING_DEST)) turnToDestination();
        }
    }

    protected void handleServerStatusUpdate(ServerHandshakeStatusUpdateEvent msg) {
        String capabilityId = msg.getMachineId();
        fuStateInfo.updateServerHandshakeState(capabilityId, msg.getStatus());
        if (msg.getStatus() == ServerSideStates.IDLE_EMPTY || msg.getStatus() == ServerSideStates.IDLE_LOADED) {
            //We can start the handshake now
            if (process.isInState(ProcessStates.HANDSHAKE_SOURCE)) performHandshakeSource();
            if (process.isInState(ProcessStates.HANDSHAKE_DEST)) performHandshakeDestination();
        }
        if (msg.getStatus() == ServerSideStates.EXECUTE) {
            if (process.isInState(ProcessStates.HANDSHAKE_SOURCE))
                fireProcessStepTransition(ProcessTriggers.CONVEY_SOURCE);
            if (process.isInState(ProcessStates.HANDSHAKE_DEST))
                fireProcessStepTransition(ProcessTriggers.CONVEY_DESTINATION);
        }
    }

    protected void handleClientStatusUpdate(ClientHandshakeStatusUpdateEvent msg) {
        String capabilityId = msg.getMachineId();
        fuStateInfo.updateClientHandshakeState(capabilityId, msg.getStatus());
        //fuStateInfo.getHandshakeEndpointInfo().updateClientHandshakeState(capabilityId, msg.getStatus());
        if (msg.getStatus() == ClientSideStates.IDLE) {
            //We can start the handshake now
            if (process.isInState(ProcessStates.HANDSHAKE_SOURCE)) performHandshakeSource();
            if (process.isInState(ProcessStates.HANDSHAKE_DEST)) performHandshakeDestination();
        }
    }

    protected void activateTransportProcess() {
        fireProcessStepTransition(ProcessTriggers.TURN_TO_SOURCE);
    }

    protected void turnToSource() {
        TransportDestinations source = getTransportDestinationForCapability(currentRequest.getCapabilityInstanceIdFrom());
        getConnectorForId(TurningCapability.CAPABILITY_ID).publish(new TurnRequest(this.componentId, source));
    }

    protected void prepareHandshakeSource() {
        String source = this.currentRequest.getCapabilityInstanceIdFrom();
        FUConnector sourceHandshake = getConnectorForId(source);
        if (sourceHandshake != null) {
            sourceHandshake.publish(new ResetRequest(componentId));
        }
    }

    protected void performHandshakeSource() {
        String capabilityIdSource = currentRequest.getCapabilityInstanceIdFrom();
        if (fuStateInfo.getHandshakeEndpointInfo().isClientHandshake(capabilityIdSource))
            getConnectorForId(capabilityIdSource).publish(new PerformHandshake(componentId));
        //We only request on the client side, since the server will start once he gets a request from remote
    }

    protected void conveyPalletSource() {
        if (fuStateInfo.isTransportAreaEmpty()) {
            getConnectorForId(ConveyorCapability.CAPABILITY_ID).publish(new LoadConveyorRequest(componentId));
        } else {
            getConnectorForId(ConveyorCapability.CAPABILITY_ID).publish(new UnloadConveyorRequest(componentId));
        }
    }

    protected void completeHandshakeSource() {
        getConnectorForId(currentRequest.getCapabilityInstanceIdFrom()).publish(new CompleteHandshake(componentId));
    }

    protected void resetTurningForDestination() {
        getConnectorForId(TurningCapability.CAPABILITY_ID).publish(new ResetRequest(componentId));
    }

    protected void turnToDestination() {
        TransportDestinations destination = getTransportDestinationForCapability(currentRequest.getCapabilityInstanceIdTo());
        getConnectorForId(TurningCapability.CAPABILITY_ID).publish(new TurnRequest(this.componentId, destination));
    }

    protected void prepareHandshakeDestination() {
        String source = this.currentRequest.getCapabilityInstanceIdTo();
        FUConnector destinationHandshake = getConnectorForId(source);
        if (destinationHandshake != null) {
            destinationHandshake.publish(new ResetRequest(componentId));
        }
    }

    protected void performHandshakeDestination() {
        String capabilityIdSource = currentRequest.getCapabilityInstanceIdTo();
        if (fuStateInfo.getHandshakeEndpointInfo().isClientHandshake(capabilityIdSource))
            getConnectorForId(capabilityIdSource).publish(new PerformHandshake(componentId));
    }

    protected void conveyPalletDestination() {
        if (fuStateInfo.isTransportAreaEmpty()) {
            getConnectorForId(ConveyorCapability.CAPABILITY_ID).publish(new LoadConveyorRequest(componentId));
        } else {
            getConnectorForId(ConveyorCapability.CAPABILITY_ID).publish(new UnloadConveyorRequest(componentId));
        }
    }

    protected void completeHandshakeDestination() {
        getConnectorForId(currentRequest.getCapabilityInstanceIdTo()).publish(new CompleteHandshake(componentId));
    }

    protected void doCompleting() {
        fireIfPossible(TurntableTriggers.COMPLETE);
        complete();
    }

    protected void complete() {
        fireIfPossible(TurntableTriggers.COMPLETING_DONE);
    }

    protected void cleanupProcess() {
        fireProcessStepTransition(ProcessTriggers.STOP_PROCESS);
    }

    protected FUConnector getConnectorForId(String capabilityId) {
        return machineChildFUs.getFUConnectorForCapabilityId(capabilityId);
    }

    protected TransportDestinations getTransportDestinationForCapability(String capabilityId) {
        if (capabilityId.startsWith("NORTH")) {
            return TransportDestinations.NORTH;
        } else if (capabilityId.startsWith("EAST")) {
            return TransportDestinations.EAST;
        } else if (capabilityId.startsWith("SOUTH")) {
            return TransportDestinations.SOUTH;
        } else if (capabilityId.startsWith("WEST")) {
            return TransportDestinations.WEST;
        } else {
            return TransportDestinations.UNKNOWN;
        }
    }

    private void fireProcessStepTransition(ProcessTriggers trigger) {
        log.info("Received trigger: " + trigger.toString());
        if (process.canFire(trigger)) process.fire(trigger);
    }

    private void fireIfPossible(TurntableTriggers trigger) {
        log.info("Received trigger: " + trigger.toString());
        if (stateMachine.canFire(trigger)) stateMachine.fire(trigger);
    }

    /**
     * A default implementation to set the Status value. Override this method if necessary for e.g. opcua
     * The default implementation only logs the message in debug mode
     *
     * @param newStatus The conveyor state as a string
     */
    @Override
    public void setStatusValue(String newStatus) {
        log.info("Current State=" + newStatus);
    }

    //Currently, no actual use
    protected void publishCurrentProcessStep(String processStep) {
        log.info("Current Process Step=" + processStep);
    }

    protected void publishCurrentState(BasicMachineStates state) {
        if (machineEventBus != null)
            //This component does not publish the state to the
            machineEventBus.publish(new MachineStatusUpdateEvent(this.componentId,
                    OPCUABasicMachineBrowsenames.STATE_VAR_NAME, "Machine State has been updated", state));
        setStatusValue(state.name());
    }
}
