package fiab.plotter.plotting;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.StatePublisher;
import fiab.core.capabilities.functionalunit.ResetRequest;
import fiab.core.capabilities.functionalunit.StopRequest;
import fiab.functionalunit.connector.FUConnector;
import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.plotter.FUStateInfo;
import fiab.plotter.message.HomingPositionReachedEvent;
import fiab.plotter.message.PlotImageRequest;
import fiab.plotter.message.PlottingStatusUpdateEvent;
import fiab.plotter.plotting.statemachine.PlotterFUTriggers;
import fiab.plotter.plotting.statemachine.PlotterStateMachine;

import java.time.Duration;

public class PlotterActor extends AbstractActor implements PlottingCapability, StatePublisher {

    //This component is not implemented properly
    //For now we just mock away all functionality and simulate motor movements via delays

    public static Props props(FUConnector plottingConnector, IntraMachineEventBus intraMachineBus) {
        return Props.create(PlotterActor.class, () -> new PlotterActor(plottingConnector, intraMachineBus));
    }

    protected final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private final PlotterStateMachine stateMachine;
    private FUConnector plottingConnector;
    private IntraMachineEventBus intraMachineBus;
    private String currentImageId;
    private String currentOrderId;

    public PlotterActor(FUConnector plottingConnector, IntraMachineEventBus intraMachineBus) {
        this.plottingConnector = plottingConnector;
        this.intraMachineBus = intraMachineBus;
        this.stateMachine = new PlotterStateMachine(new FUStateInfo(self()));

        addActionsToStates();
        publishCurrentState();
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(ResetRequest.class, req -> fireIfPossible(PlotterFUTriggers.RESET))
                .match(StopRequest.class, req -> fireIfPossible(PlotterFUTriggers.STOP))
                .match(PlotImageRequest.class, req -> plot(req.getImageId(), req.getImageId()))
                .match(HomingPositionReachedEvent.class, req -> handleHomingPositionReached())
                .build();
    }

    public void addActionsToStates() {
        for (BasicMachineStates state : BasicMachineStates.values()) {
            this.stateMachine.configure(state).onEntry(this::publishCurrentState);
        }
        this.stateMachine.configure(BasicMachineStates.STOPPING).onEntry(this::doStopping);
        this.stateMachine.configure(BasicMachineStates.RESETTING).onEntry(this::doResetting);
        this.stateMachine.configure(BasicMachineStates.EXECUTE).onEntry(this::simulateHardware);
        this.stateMachine.configure(BasicMachineStates.COMPLETING).onEntry(this::resetMotorsToHomePosition);
    }

    @Override
    public void doResetting() {
        fireIfPossible(PlotterFUTriggers.RESET);
        log.info("Started homing motors for reset");
        resetMotorsToHomePosition();
    }

    @Override
    public void doStopping() {
        log.info("Motors stopped");
        fireIfPossible(PlotterFUTriggers.STOP_DONE);
    }

    @Override
    public void plot(String imageId, String orderId) {
        log.info("Preparing to plot image with id=" + imageId + ", for orderId=" + orderId);
        fireIfPossible(PlotterFUTriggers.START);
        this.currentImageId = imageId;
        this.currentOrderId = orderId;
        fireIfPossible(PlotterFUTriggers.EXECUTE);
    }

    public void simulateHardware() {
        log.info("Simulating plotter movements by using a delay of 1 second(s)");
        context().system().scheduler().scheduleOnce(Duration.ofSeconds(1), () -> {
            fireIfPossible(PlotterFUTriggers.COMPLETE);
        }, context().dispatcher());
    }

    public void resetMotorsToHomePosition() {
        log.info("Simulating homing movement by using a delay of 1 second(s)");
        context().system().scheduler().scheduleOnce(Duration.ofSeconds(1), () -> {
            self().tell(new HomingPositionReachedEvent(), self());
        }, context().dispatcher());
    }

    public void handleHomingPositionReached() {
        log.info("Homing Position reached in state " + stateMachine.getState());
        if (stateMachine.isInState(BasicMachineStates.RESETTING)) {
            fireIfPossible(PlotterFUTriggers.RESET_DONE);
        } else if (stateMachine.isInState(BasicMachineStates.COMPLETING)) {
            fireIfPossible(PlotterFUTriggers.COMPLETING_DONE);
        } else {
            fireIfPossible(PlotterFUTriggers.STOP);
        }
    }

    private void fireIfPossible(PlotterFUTriggers trigger) {
        log.info("Received trigger: " + trigger.toString());
        if (stateMachine.canFire(trigger))
            stateMachine.fire(trigger);
    }

    @Override
    public void setStatusValue(String newStatus) {
        log.debug("Current State=" + newStatus);
    }

    private void publishCurrentState() {
        if (intraMachineBus != null)
            intraMachineBus.publish(new PlottingStatusUpdateEvent(self().path().name(), stateMachine.getState()));
        setStatusValue(stateMachine.getState().toString());
    }


}
