package fiab.plotter;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import fiab.conveyor.ConveyorCapability;
import fiab.conveyor.messages.ConveyorStatusUpdateEvent;
import fiab.conveyor.messages.LoadConveyorRequest;
import fiab.conveyor.messages.UnloadConveyorRequest;
import fiab.conveyor.statemachine.ConveyorStates;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.StatePublisher;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.functionalunit.ResetRequest;
import fiab.core.capabilities.functionalunit.StopRequest;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.core.capabilities.handshake.client.CompleteHandshake;
import fiab.core.capabilities.handshake.server.TransportAreaStatusOverrideRequest;
import fiab.core.capabilities.plotting.PlotRequest;
import fiab.core.capabilities.plotting.WellknownPlotterCapability;
import fiab.functionalunit.MachineChildFUs;
import fiab.functionalunit.connector.FUConnector;
import fiab.functionalunit.connector.FUSubscriptionClassifier;
import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.functionalunit.connector.MachineEventBus;
import fiab.handshake.server.messages.ServerHandshakeStatusUpdateEvent;
import fiab.plotter.plotting.message.PlotImageRequest;
import fiab.plotter.plotting.message.PlottingStatusUpdateEvent;
import fiab.plotter.plotting.PlottingCapability;
import fiab.plotter.statemachine.PlotterCoordinatorStateMachine;
import fiab.plotter.statemachine.PlotterTriggers;

public class PlotterCoordinatorActor extends AbstractActor implements WellknownPlotterCapability, StatePublisher {

    public static Props props(MachineEventBus machineEventBus, IntraMachineEventBus intraMachineEventBus, MachineChildFUs childFUs) {
        return Props.create(PlotterCoordinatorActor.class,
                () -> new PlotterCoordinatorActor(machineEventBus, intraMachineEventBus, childFUs));
    }

    protected final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    protected final String componentId;
    protected final PlotterCoordinatorStateMachine stateMachine;
    private MachineEventBus machineEventBus;
    private IntraMachineEventBus intraMachineEventBus;
    private MachineChildFUs childFUs;
    private FUStateInfo fuStateInfo;
    private PlotRequest currentPlotRequest;

    protected PlotterCoordinatorActor(MachineEventBus machineEventBus, IntraMachineEventBus intraMachineEventBus, MachineChildFUs childFUs) {
        this.componentId = self().path().name();
        this.machineEventBus = machineEventBus;
        this.intraMachineEventBus = intraMachineEventBus;
        this.childFUs = childFUs;
        this.fuStateInfo = new FUStateInfo(self());
        this.stateMachine = new PlotterCoordinatorStateMachine(fuStateInfo);
        init();
        publishCurrentState(stateMachine.getState());
    }

    private void init() {
        addActionsToStates();
        subscribeToConnectors();
    }

    private void addActionsToStates() {
        for (BasicMachineStates state : BasicMachineStates.values()) {
            this.stateMachine.configure(state).onEntry(() -> publishCurrentState(state));
        }
        this.stateMachine.configure(BasicMachineStates.STOPPING).onEntry(this::doStopping);
        this.stateMachine.configure(BasicMachineStates.RESETTING).onEntry(this::doResetting);
        this.stateMachine.configure(BasicMachineStates.EXECUTE).onEntry(this::drawImage);
    }

    private void subscribeToConnectors() {
        FUSubscriptionClassifier FUSubscriptionClassifier = new FUSubscriptionClassifier(this.componentId, "*");
        this.intraMachineEventBus.subscribe(self(), FUSubscriptionClassifier);
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(ResetRequest.class, req -> fireIfPossible(PlotterTriggers.RESET))
                .match(StopRequest.class, req -> fireIfPossible(PlotterTriggers.STOP))
                .match(PlotRequest.class, req -> handlePlotRequest(req))
                .match(PlottingStatusUpdateEvent.class, msg -> handlePlottingFUStatusUpdate(msg))
                .match(ConveyorStatusUpdateEvent.class, msg -> handleConveyorFUStatusUpdateEvent(msg))
                .match(ServerHandshakeStatusUpdateEvent.class, msg -> handleServerHandshakeStatusUpdate(msg))
                .build();
    }

    @Override
    public void doResetting() {
        FUConnector conveyorConnector = childFUs.getFUConnectorForCapabilityId(ConveyorCapability.CAPABILITY_ID);
        conveyorConnector.publish(new ResetRequest(this.componentId));
        FUConnector plottingConnector = childFUs.getFUConnectorForCapabilityId(PlottingCapability.CAPABILITY_ID);
        plottingConnector.publish(new ResetRequest(this.componentId));
    }

    @Override
    public void doStopping() {
        for (FUConnector connector : childFUs.getFuConnectors().values()) {
            connector.publish(new StopRequest(this.componentId));
        }
    }

    public void handlePlotRequest(PlotRequest request) {
        this.currentPlotRequest = request;
        fireIfPossible(PlotterTriggers.START);
        sender().tell(new MachineStatusUpdateEvent(this.componentId, "state", "status update",stateMachine.getState()), self());
        loadImage(request.getImageId());
        performLoadingHandshake();
    }

    public void handlePlottingFUStatusUpdate(PlottingStatusUpdateEvent plottingStatusUpdateEvent) {
        fuStateInfo.setPlottingState(plottingStatusUpdateEvent.getStatus());
        if (plottingStatusUpdateEvent.getStatus() == BasicMachineStates.COMPLETE) {
            //Free up memory here if image id is stored
            fireIfPossible(PlotterTriggers.COMPLETE);
            performUnloadingHandshake();
        }
    }

    public void handleConveyorFUStatusUpdateEvent(ConveyorStatusUpdateEvent conveyorStatusUpdateEvent) {
        fuStateInfo.setConveyorFUState(conveyorStatusUpdateEvent.getStatus());
        if (stateMachine.isInState(BasicMachineStates.STARTING)
                && conveyorStatusUpdateEvent.getStatus() == ConveyorStates.IDLE_FULL) {
            FUConnector connector = childFUs.getFUConnectorForCapabilityId(HandshakeCapability.SERVER_CAPABILITY_ID);
            connector.publish(new TransportAreaStatusOverrideRequest(this.componentId, HandshakeCapability.StateOverrideRequests.SetLoaded));
            completeHandshake();
            fireIfPossible(PlotterTriggers.EXECUTE);
        }
        if (stateMachine.isInState(BasicMachineStates.COMPLETING) &&
                conveyorStatusUpdateEvent.getStatus() == ConveyorStates.IDLE_EMPTY) {
            FUConnector connector = childFUs.getFUConnectorForCapabilityId(HandshakeCapability.SERVER_CAPABILITY_ID);
            connector.publish(new TransportAreaStatusOverrideRequest(this.componentId, HandshakeCapability.StateOverrideRequests.SetEmpty));
            completeHandshake();
            fireIfPossible(PlotterTriggers.COMPLETING_DONE);
        }
    }

    public void handleServerHandshakeStatusUpdate(ServerHandshakeStatusUpdateEvent handshakeStatusUpdateEvent) {
        fuStateInfo.setHandshakeFUState(handshakeStatusUpdateEvent.getStatus());
        if (stateMachine.isInState(BasicMachineStates.STARTING) && handshakeStatusUpdateEvent.getStatus() == ServerSideStates.EXECUTE) {
            loadPallet();
        }
        if (stateMachine.isInState(BasicMachineStates.COMPLETING)
                && handshakeStatusUpdateEvent.getStatus() == ServerSideStates.EXECUTE) {
            unloadPallet();
        }
    }

    private void loadImage(String imageId) {
        log.info("Loading image with id {}", imageId);
    }

    private void performLoadingHandshake() {
        FUConnector connector = childFUs.getFUConnectorForCapabilityId(HandshakeCapability.SERVER_CAPABILITY_ID);
        connector.publish(new ResetRequest(this.componentId));  //Will be completed by client hs
    }

    private void loadPallet() {
        FUConnector connector = childFUs.getFUConnectorForCapabilityId(ConveyorCapability.CAPABILITY_ID);
        connector.publish(new LoadConveyorRequest(this.componentId));
    }

    private void drawImage() {
        FUConnector connector = childFUs.getFUConnectorForCapabilityId(PlottingCapability.CAPABILITY_ID);
        connector.publish(new PlotImageRequest(currentPlotRequest.getSenderId(), currentPlotRequest.getImageId(), "NOT_IMPLEMENTED"));
    }

    private void performUnloadingHandshake() {
        FUConnector connector = childFUs.getFUConnectorForCapabilityId(HandshakeCapability.SERVER_CAPABILITY_ID);
        connector.publish(new ResetRequest(this.componentId));
    }

    private void unloadPallet() {
        FUConnector connector = childFUs.getFUConnectorForCapabilityId(ConveyorCapability.CAPABILITY_ID);
        connector.publish(new UnloadConveyorRequest(this.componentId));
    }

    private void completeHandshake(){
        FUConnector connector = childFUs.getFUConnectorForCapabilityId(HandshakeCapability.SERVER_CAPABILITY_ID);
        connector.publish(new CompleteHandshake(this.componentId));
    }

    private void fireIfPossible(PlotterTriggers trigger) {
        log.info("Received trigger: " + trigger.toString());
        if (stateMachine.canFire(trigger)) stateMachine.fire(trigger);
    }

    private void publishCurrentState(BasicMachineStates state) {
        if (machineEventBus != null) {
            machineEventBus.publish(new MachineStatusUpdateEvent(this.componentId,
                    OPCUABasicMachineBrowsenames.STATE_VAR_NAME, "Machine State has been updated", state));
            setStatusValue(state.name());
        }
    }

    @Override
    public void setStatusValue(String newStatus) {
        log.info("Current State=" + newStatus);
    }

}
