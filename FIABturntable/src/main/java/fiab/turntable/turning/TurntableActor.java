package fiab.turntable.turning;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.github.oxo42.stateless4j.StateMachine;

import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.StatePublisher;
import fiab.core.capabilities.basicmachine.BasicMachineRequests;
import fiab.turntable.actor.IntraMachineEventBus;
import hardware.TurningHardware;
import hardware.lego.LegoTurningHardware;
import hardware.mock.TurningMockHardware;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;

import static fiab.turntable.turning.TurningStates.STOPPED;
import static fiab.turntable.turning.TurningTriggers.*;

import java.time.Duration;

public class TurntableActor extends AbstractActor {

    //In case the operating system is windows, we do not want to use EV3 libraries
    private static final boolean DEBUG = System.getProperty("os.name").toLowerCase().contains("win");
    private final int timeForNinetyDeg = 1325;

    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private TurningHardware turningHardware;

    private IntraMachineEventBus intraEventBus;
    private StatePublisher publishEP;

    protected TurnTableOrientation orientation;
    protected StateMachine<TurningStates, TurningTriggers> tsm;

    public static Props props(IntraMachineEventBus intraEventBus, StatePublisher publishEP) {
        return Props.create(TurntableActor.class, () -> new TurntableActor(intraEventBus, publishEP));
    }

    public TurntableActor(IntraMachineEventBus intraEventBus, StatePublisher publishEP) {
        this.intraEventBus = intraEventBus;
        this.publishEP = publishEP;
        this.tsm = new StateMachine<>(STOPPED, new TurningStateMachineConfig());
        this.orientation = TurnTableOrientation.NORTH;
        Runtime.getRuntime().addShutdownHook(new Thread(this::motorStop));
        initHardware();
        publishNewState();
    }

    private void initHardware() {
        if (DEBUG) {
            turningHardware = new TurningMockHardware(200);
        } else {
            turningHardware = new LegoTurningHardware(MotorPort.D, SensorPort.S4);
            turningHardware.getTurningMotor().setSpeed(200);
        }
    }

    @Override
    public Receive createReceive() {
    	return receiveBuilder()
    			.match(TurningTriggers.class, req -> {
    				switch(req) {
    				case STOP:
    					if (tsm.canFire(STOP)) {
    						tsm.fire(STOP);
    						publishNewState();  //in STOPPING
    						stop();
    					} 
    					break;
    				case RESET:
    					if (tsm.canFire(RESET)) {
    						tsm.fire(RESET);    //in RESETTING
    						publishNewState();
    						reset();
    					}
    					break;
    				}
    			}).match(TurnRequest.class, req -> {
    				if (tsm.canFire(TURN_TO)) {
    					tsm.fire(TURN_TO);  //in STARTING
    					publishNewState();
    					turn(req);
    				}
    			})
    			.matchAny(msg -> {
    				log.warning("Unexpected Message received: " + msg.toString());
    			})
    			.build();
    }

    private void publishNewState() {
        if (publishEP != null)
            publishEP.setStatusValue(tsm.getState().toString());
        if (intraEventBus != null) {
            intraEventBus.publish(new TurntableStatusUpdateEvent("", OPCUABasicMachineBrowsenames.STATE_VAR_NAME, "", tsm.getState()));
        }
    }

    private void stop() {
        motorStop();
        context().system()
                .scheduler()
                .scheduleOnce(Duration.ofMillis(1000),
                        () -> {
                            tsm.fire(NEXT);
                            publishNewState();
                        }, context().system().dispatcher());
    }

    private void reset() {
        motorBackward();
        checkHomingPositionReached();
    }

    /**
     * Checks periodically whether homing position is reached. If reached, machine goes idle
     */
    private void checkHomingPositionReached() {
        if (sensorHomingHasDetectedInput()) {
            motorStop();
            orientation = TurnTableOrientation.NORTH;
            tsm.fire(NEXT);
            publishNewState();
        } else {
            context().system().scheduler().scheduleOnce(Duration.ofMillis(100),
                    this::checkHomingPositionReached
                    , context().system().dispatcher());
        }
    }

    private void turn(TurnRequest treq) {
        tsm.fire(EXECUTE);
        publishNewState();
        // Do actual turning here
        TurnTableOrientation target = treq.getTto();
        context().system().scheduler().scheduleOnce(Duration.ofMillis(100), () -> turnTo(target), context().system().dispatcher());
       // checkTurningPositionReached(target); // this is called by TurnTo anyway
    }

    private void completing() {
        tsm.fire(NEXT);
        publishNewState();       //we are now in COMPLETING
        complete();
    }

    private void complete() {
        tsm.fire(NEXT);
        publishNewState();       //we are now in COMPLETE
        autoResetToIdle();
    }

    private void autoResetToIdle() {
        tsm.fire(NEXT);
        publishNewState();       //we are now in IDLE
    }

    private void turnTo(TurnTableOrientation target) {
        if (target.getNumericValue() > this.orientation.getNumericValue()) {
            turnRight(target);
        } else if (target.getNumericValue() < this.orientation.getNumericValue()) {
            turnLeft(target);
        }
        checkTurningPositionReached(target);
    }

    private void turnLeft(TurnTableOrientation target) {
        if (this.orientation == TurnTableOrientation.NORTH) {
            log.warning("Cannot turn left from North");
        }
        log.debug("Executing from turning: turnLeft");
        motorBackward();
        context().system().scheduler().scheduleOnce(Duration.ofMillis(timeForNinetyDeg),
                () -> {
                    motorStop();
                    orientation = orientation.getNextCounterClockwise(orientation);
                    log.debug("Orientation is now: " + orientation);
                    turnTo(target);
                },
                context().system().dispatcher());
        //this.turnMotor.rotate(-rotationToNext);
    }

    /**
     * Turns right by the amount of degrees specified in rotationToNext
     */
    private void turnRight(TurnTableOrientation target) {
        if (this.orientation == TurnTableOrientation.WEST) {
            log.warning("Cannot turn right from West");
            return;
        }
        log.debug("Executing from turning: turnRight");
        motorForward();
        context().system().scheduler().scheduleOnce(Duration.ofMillis(timeForNinetyDeg),
                () -> {
                    motorStop();
                    orientation = orientation.getNextClockwise(orientation);
                    log.debug("Orientation is now: " + orientation);
                    turnTo(target);
                },
                context().system().dispatcher());
    }

    private void checkTurningPositionReached(TurnTableOrientation orientation) {
    	if (this.tsm.isInState(TurningStates.EXECUTING)) {
    		if (this.orientation == orientation) {
    			completing();
    		} else {
    			context().system().scheduler().scheduleOnce(Duration.ofMillis(100),
    					() -> checkTurningPositionReached(orientation)
    					, context().system().dispatcher());
    		}
    	}
    }

    private boolean sensorHomingHasDetectedInput() {
        return turningHardware.getSensorHoming().hasDetectedInput();
    }

    private void motorForward() {
        turningHardware.getTurningMotor().forward();
    }

    private void motorBackward() {
        turningHardware.getTurningMotor().backward();
    }

    private void motorStop() {
        turningHardware.getTurningMotor().stop();
    }
}
