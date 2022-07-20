package fiab.conveyor;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import com.github.oxo42.stateless4j.StateMachine;
import fiab.conveyor.messages.ConveyorStatusUpdateEvent;
import fiab.conveyor.messages.InternalConveyorRequests;
import fiab.conveyor.messages.LoadConveyorRequest;
import fiab.conveyor.messages.UnloadConveyorRequest;
import fiab.conveyor.statemachine.ConveyorStateMachine;
import fiab.conveyor.statemachine.ConveyorStates;
import fiab.conveyor.statemachine.ConveyorTriggers;
import fiab.core.capabilities.StatePublisher;
import fiab.core.capabilities.functionalunit.ResetRequest;
import fiab.core.capabilities.functionalunit.StopRequest;
import fiab.functionalunit.connector.FUConnector;
import fiab.functionalunit.connector.FUSubscriptionClassifier;
import fiab.functionalunit.connector.IntraMachineEventBus;
import hardware.ConveyorHardware;
import hardware.lego.LegoConveyorHardware;
import hardware.mock.ConveyorMockHardware;

public class ConveyorActor extends AbstractActor implements ConveyorCapability, StatePublisher {

    protected final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    protected final StateMachine<ConveyorStates, ConveyorTriggers> stateMachine;
    protected final ConveyorHardware conveyorHardware;
    protected final IntraMachineEventBus intraMachineBus;
    protected final FUConnector conveyorConnector;

    static public Props props(FUConnector conveyorConnector, IntraMachineEventBus intraEventBus) {
        return Props.create(ConveyorActor.class, () -> new ConveyorActor(conveyorConnector, intraEventBus));
    }

    public ConveyorActor(FUConnector conveyorConnector, IntraMachineEventBus intraMachineBus) {
        this.conveyorConnector = conveyorConnector;
        this.conveyorConnector.subscribe(self(), new FUSubscriptionClassifier(self().path().name(), "*"));
        this.intraMachineBus = intraMachineBus;
        this.stateMachine = new ConveyorStateMachine();
        //In case the operating system is windows, we do not want to use EV3 libraries
        boolean debug = System.getProperty("os.name").toLowerCase().contains("win");
        this.conveyorHardware = debug ? new ConveyorMockHardware() : new LegoConveyorHardware();
        addActionsToStates();
        publishCurrentState();
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(InternalConveyorRequests.class, req -> {
                    if (req == InternalConveyorRequests.CHECK_FOR_FULLY_LOADED) checkConveyorFullyLoaded();
                    if (req == InternalConveyorRequests.CHECK_FOR_FULLY_UNLOADED) checkConveyorFullyUnloaded();
                })
                .match(StopRequest.class, req -> fireIfPossible(ConveyorTriggers.STOP))
                .match(ResetRequest.class, req -> fireIfPossible(ConveyorTriggers.RESET))
                .match(LoadConveyorRequest.class, req -> fireIfPossible(ConveyorTriggers.LOAD))
                .match(UnloadConveyorRequest.class, req -> fireIfPossible(ConveyorTriggers.UNLOAD))
                .match(ConveyorTriggers.class, this::fireIfPossible)
                .build();
    }

    public void addActionsToStates() {
        for (ConveyorStates state : ConveyorStates.values()) {
            this.stateMachine.configure(state).onEntry(this::publishCurrentState);
        }
        this.stateMachine.configure(ConveyorStates.STOPPING).onEntry(this::doStopping);
        this.stateMachine.configure(ConveyorStates.RESETTING).onEntry(this::doResetting);
        this.stateMachine.configure(ConveyorStates.LOADING).onEntry(this::loadConveyor);
        this.stateMachine.configure(ConveyorStates.UNLOADING).onEntry(this::unloadConveyor);
    }

    @Override
    public void doResetting() {
        conveyorHardware.stopConveyorMotor();
        if (conveyorHardware.isLoadingSensorDetectingPallet() || conveyorHardware.isUnloadingSensorDetectingPallet()) {
            fireIfPossible(ConveyorTriggers.RESET_DONE_FULL);
        } else fireIfPossible(ConveyorTriggers.RESET_DONE_EMPTY);
    }

    @Override
    public void doStopping() {
        conveyorHardware.stopConveyorMotor();
        fireIfPossible(ConveyorTriggers.STOP_DONE);
    }

    public void loadConveyor() {
        conveyorHardware.startMotorForLoading();
        checkConveyorFullyLoaded();
    }

    protected void checkConveyorFullyLoaded() {
        if (conveyorHardware.isLoadingSensorDetectingPallet() && stateMachine.getState() == ConveyorStates.LOADING)
            fireIfPossible(ConveyorTriggers.LOADING_DONE);
        else self().tell(InternalConveyorRequests.CHECK_FOR_FULLY_LOADED, self());
    }

    @Override
    public void unloadConveyor() {
        conveyorHardware.startMotorForUnloading();
        checkConveyorFullyUnloaded();
    }

    protected void checkConveyorFullyUnloaded() {
        if (!conveyorHardware.isLoadingSensorDetectingPallet()
                && !conveyorHardware.isUnloadingSensorDetectingPallet()
                && stateMachine.getState() == ConveyorStates.UNLOADING) {
            fireIfPossible(ConveyorTriggers.UNLOADING_DONE);
        } else self().tell(InternalConveyorRequests.CHECK_FOR_FULLY_UNLOADED, self());
    }

    protected void fireIfPossible(ConveyorTriggers trigger) {
        log.info("Received trigger: " + trigger.toString());
        if (stateMachine.canFire(trigger)) {
            stateMachine.fire(trigger);
        } else {
            log.debug("Skipping trigger " + trigger + " due to incompatible state" + stateMachine.getState());
        }
    }

    protected void publishCurrentState() {
        if (intraMachineBus != null)
            intraMachineBus.publish(new ConveyorStatusUpdateEvent(self().path().name(), stateMachine.getState()));
        setStatusValue(stateMachine.getState().toString());
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
}
