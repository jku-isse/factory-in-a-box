package fiab.turntable.turning;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.github.oxo42.stateless4j.StateMachine;
import fiab.core.capabilities.StatePublisher;
import fiab.core.capabilities.functionalunit.ResetRequest;
import fiab.core.capabilities.functionalunit.StopRequest;
import fiab.functionalunit.connector.FUConnector;
import fiab.functionalunit.connector.FUSubscriptionClassifier;
import fiab.core.capabilities.transport.TransportDestinations;
import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.turntable.turning.messages.InternalTurningRequests;
import fiab.turntable.turning.messages.TurnRequest;
import fiab.turntable.turning.messages.TurningStatusUpdateEvent;
import fiab.turntable.turning.statemachine.TurningStateMachine;
import fiab.turntable.turning.statemachine.TurningStates;
import fiab.turntable.turning.statemachine.TurningTriggers;
import hardware.TurningHardware;
import hardware.lego.LegoTurningHardware;
import hardware.mock.TurningMockHardware;

import static fiab.turntable.turning.statemachine.TurningTriggers.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class TurningActor extends AbstractActor implements TurningCapability, StatePublisher {

    protected final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    protected final Map<TransportDestinations, Integer> positionMap;

    protected IntraMachineEventBus intraMachineBus;
    protected FUConnector turningConnector;
    protected StateMachine<TurningStates, TurningTriggers> stateMachine;
    protected TurningHardware turningHardware;

    protected TransportDestinations currentDestination = TransportDestinations.UNKNOWN;
    protected int gearRatio = 3;    //The current hardware has approx. a 3:1 gear ratio we need to consider

    public static Props props(FUConnector turningConnector, IntraMachineEventBus intraMachineEventBus) {
        return Props.create(TurningActor.class, () -> new TurningActor(turningConnector, intraMachineEventBus));
    }

    public TurningActor(FUConnector turningConnector, IntraMachineEventBus intraEventBus) {
        this.turningConnector = turningConnector;
        this.turningConnector.subscribe(self(), new FUSubscriptionClassifier(self().path().name(), "*"));
        this.intraMachineBus = intraEventBus;

        this.stateMachine = new TurningStateMachine();
        this.positionMap = new HashMap<>();
        mapMotorAnglesToPositions();
        //In case the operating system not ev3, we do not want to use EV3 libraries
        //For now this hack will let us build on any os where username is not robot
        String userName = System.getProperty("user.name");
        log.debug("User Name {}", userName);
        boolean debug = !userName.equalsIgnoreCase("robot");    //All EV3 distros use this as default username
        log.info("TurningActor using Mock hardware? {}", debug);
        this.turningHardware = debug ? new TurningMockHardware() : new LegoTurningHardware();
        this.turningHardware.getTurningMotor().setSpeed(200);
        addActionsToStates();
        publishCurrentState();
    }

    public void addActionsToStates() {
        for (TurningStates state : TurningStates.values()) {
            this.stateMachine.configure(state).onEntry(this::publishCurrentState);
        }
        this.stateMachine.configure(TurningStates.STOPPING).onEntry(this::doStopping);
        this.stateMachine.configure(TurningStates.RESETTING).onEntry(this::doResetting);
        this.stateMachine.configure(TurningStates.EXECUTING).onEntry(this::turnToDestination);
        this.stateMachine.configure(TurningStates.COMPLETING).onEntry(this::finishTurning);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ResetRequest.class, req -> fireIfPossible(RESET))
                .match(StopRequest.class, req -> fireIfPossible(STOP))
                .match(TurningTriggers.class, this::fireIfPossible)
                .match(TurnRequest.class, this::turnTo)
                .match(InternalTurningRequests.class, this::handleInternalRequest)
                .build();
    }

    public void handleInternalRequest(InternalTurningRequests request) {
        if (request == InternalTurningRequests.CHECK_RESET_POSITION_REACHED) {
            checkIfHomingPositionReached();
        } else if (request == InternalTurningRequests.CHECK_DESTINATION_REACHED) {
            checkDestinationReached();
        }
    }

    public void doStopping() {
        turningHardware.stopTurningMotor();
        fireIfPossible(STOPPING_DONE);
    }

    public void doResetting() {
        currentDestination = TransportDestinations.UNKNOWN;
        turningHardware.getTurningMotor().setSpeed(200);
        turningHardware.startMotorBackward();
        checkIfHomingPositionReached();
    }

    public void turnTo(TurnRequest request) {
        if (stateMachine.canFire(START)) {
            stateMachine.fire(START);
            this.currentDestination = request.getTarget();
            //Placeholder in case we need to do something before turning
            fireIfPossible(EXECUTE);
        } else {
            log.warning("Turning Unit in state " + stateMachine.getState() + " not ready for TurningRequest to: " + request.getTarget());
        }
    }

    public void checkIfHomingPositionReached() {
        if (turningHardware.isInHomePosition()) {
            stopAndResetTachoCount();
            context().system().scheduler().scheduleOnce(Duration.ofMillis(100), () -> {
                fireIfPossible(RESETTING_DONE);
            }, context().dispatcher());
        } else {
            /*if (!turningHardware.getTurningMotor().isRunning()) {
                log.info("Motor was not moving, retrying");
                context().system().scheduler().scheduleOnce(Duration.ofMillis(500), () -> {
                    turningHardware.startMotorBackward();   //In case something prevented us from moving the motor before
                    self().tell(InternalTurningRequests.CHECK_RESET_POSITION_REACHED, self());
                }, context().dispatcher());
            } else {*/
            self().tell(InternalTurningRequests.CHECK_RESET_POSITION_REACHED, self());
            //}
        }
    }

    public void turnToDestination() {
        log.info("Starting turning to destination: " + currentDestination);
        turningHardware.getTurningMotor().setSpeed(200);    //Resets each time after tacho count reset
        switch (currentDestination) {
            case NORTH:
                turningHardware.startMotorBackward();
                break;
            case EAST:  //For now we do the same for all others
            case SOUTH:
            case WEST:
                turningHardware.rotateMotorToAngle(positionMap.get(currentDestination));
                break;
            default:
                log.error("Cannot find position for " + currentDestination);
                return;
        }
        checkDestinationReached();
    }

    public void checkDestinationReached() {
        if (stateMachine.isInState(TurningStates.EXECUTING) && currentDestination == TransportDestinations.NORTH
                && turningHardware.isInHomePosition()) {    //We check the sensor instead when moving north
            stopAndResetTachoCount();
            log.info("Turning Position " + currentDestination + " reached");
            fireIfPossible(COMPLETE);
        } else if (stateMachine.isInState(TurningStates.EXECUTING) && currentDestination != TransportDestinations.NORTH
                && hasReachedTargetRotation(5)) {    //After calling rotate, the motor will stop upon reaching it's destination
            //&& turningHardware.getMotorAngle() == positionMap.get(currentDestination)) {
            log.info("Turning Position {} reached", currentDestination);
            fireIfPossible(COMPLETE);
        } else {
            self().tell(InternalTurningRequests.CHECK_DESTINATION_REACHED, self());
        }
    }

    private boolean hasReachedTargetRotation(int delta) {
        int motorAngle = turningHardware.getMotorAngle();
        int target = positionMap.get(currentDestination);
        int lowerBound = target - delta;
        int upperBound = target + delta;
        return lowerBound <= motorAngle && motorAngle <= upperBound;
    }

    public void finishTurning() {
        context().system().scheduler().scheduleOnce(Duration.ofMillis(100), () -> {
            fireIfPossible(COMPLETING_DONE);
        }, context().dispatcher());
    }

    /**
     * A default implementation to set the Status value. Override this method if necessary for e.g. opcua
     * The default implementation only logs the message in debug mode
     *
     * @param newStatus The conveyor state as a string
     */
    @Override
    public void setStatusValue(String newStatus) {
        log.debug("Current State=" + newStatus);
    }

    private void fireIfPossible(TurningTriggers trigger) {
        log.info("Received trigger: " + trigger.toString());
        if (stateMachine.canFire(trigger))
            stateMachine.fire(trigger);
        else
            log.warning("Failed transition from state {} using trigger {}", stateMachine.getState(), trigger);
    }

    private void publishCurrentState() {
        if (intraMachineBus != null)
            intraMachineBus.publish(new TurningStatusUpdateEvent(self().path().name(), stateMachine.getState()));
        setStatusValue(stateMachine.getState().toString());
    }

    private void mapMotorAnglesToPositions() {
        positionMap.put(TransportDestinations.NORTH, 0 * gearRatio);
        positionMap.put(TransportDestinations.EAST, 90 * gearRatio + 10); //In theory 90, but not in reality
        positionMap.put(TransportDestinations.SOUTH, 180 * gearRatio - 15); //In theory 180, but not in reality
        positionMap.put(TransportDestinations.WEST, 270 * gearRatio - 30); //In theory 270, but not in reality
    }

    private void stopAndResetTachoCount() {
        turningHardware.stopTurningMotor();
        turningHardware.getTurningMotor().resetTachoCount();
        turningHardware.getTurningMotor().setSpeed(200);    //After tacho reset we need to set speed again
    }
}
