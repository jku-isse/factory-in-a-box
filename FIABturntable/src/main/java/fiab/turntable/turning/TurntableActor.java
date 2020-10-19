package fiab.turntable.turning;

import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.core.capabilities.StatePublisher;
import fiab.turntable.actor.IntraMachineEventBus;
import fiab.turntable.turning.statemachine.TurningStates;
import hardware.TurningHardware;
import hardware.config.HardwareConfig;
import hardware.lego.LegoTurningHardware;
import hardware.mock.TurningMockHardware;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import org.w3c.dom.html.HTMLAreaElement;

import static fiab.turntable.turning.statemachine.TurningTriggers.*;

import java.time.Duration;

public class TurntableActor extends BaseBehaviorTurntableActor {

    //In case the operating system is windows, we do not want to use EV3 libraries
    private static final boolean DEBUG = System.getProperty("os.name").toLowerCase().contains("win");
    //private final int timeForNinetyDeg = 1325;
    private final int ratio = 3;
    private final int rightAngleDeg = 90;
    //private final int NORTH_ANGLE = rightAngleDeg * TurnTableOrientation.NORTH.getNumericValue() * ratio;
    private final int EAST_ANGLE = rightAngleDeg * TurnTableOrientation.EAST.getNumericValue() * ratio + 10;
    private final int SOUTH_ANGLE = rightAngleDeg * TurnTableOrientation.SOUTH.getNumericValue() * ratio - 20;
    private final int WEST_ANGLE = rightAngleDeg * TurnTableOrientation.WEST.getNumericValue() * ratio - 30; //corrections

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private TurningHardware turningHardware;
    protected TurnTableOrientation orientation;

    public static Props props(IntraMachineEventBus intraEventBus, StatePublisher publishEP, HardwareConfig hardwareConfig) {
        return Props.create(TurntableActor.class, () -> new TurntableActor(intraEventBus, publishEP, hardwareConfig));
    }

    public TurntableActor(IntraMachineEventBus intraEventBus, StatePublisher publishEP, HardwareConfig hardwareConfig) {
        super(intraEventBus, publishEP);
        this.orientation = TurnTableOrientation.NORTH;
        Runtime.getRuntime().addShutdownHook(new Thread(this::motorStop));
        initHardware(hardwareConfig);
    }

    private void initHardware(HardwareConfig hardwareConfig) {

        /*if (DEBUG) {
            if (hardwareConfig.getMotorD().isPresent() && hardwareConfig.getSensor4().isPresent()) {
                turningHardware = new TurningMockHardware(hardwareConfig.getMotorD().get(), hardwareConfig.getSensor4().get());
            }
        } else {
            if (hardwareConfig.getMotorD().isPresent() && hardwareConfig.getSensor4().isPresent()) {
                turningHardware = new LegoTurningHardware(hardwareConfig.getMotorD().get(), hardwareConfig.getSensor4().get());
                turningHardware.getTurningMotor().setSpeed(200);
            }
        }*/
        if(hardwareConfig.getTurningHardware().isPresent()){
            turningHardware = hardwareConfig.getTurningHardware().get();
        }else{
            throw new RuntimeException("TurningHardware was not properly initialized!");
        }
    }

    protected void stop() {
        motorStop();
        context().system()
                .scheduler()
                .scheduleOnce(Duration.ofMillis(500),
                        () -> {
                            tsm.fire(NEXT);
                            publishNewState();
                        }, context().system().dispatcher());
    }

    protected void reset() {
        if (!sensorHomingHasDetectedInput()) {
            motorBackward();
        }
        checkHomingPositionReached();
    }

    /**
     * Checks periodically whether homing position is reached. If reached, machine goes idle
     */
    private void checkHomingPositionReached() {
        if (sensorHomingHasDetectedInput()) {
            motorStop();
            resetTachoCount();
            orientation = TurnTableOrientation.NORTH;
            tsm.fire(NEXT);
            publishNewState();
        } else {
            context().system().scheduler().scheduleOnce(Duration.ofMillis(20),  //Tick duration of scheduler = 10ms
                    this::checkHomingPositionReached
                    , context().system().dispatcher());
        }
    }

    protected void turn(TurnRequest treq) {
        tsm.fire(EXECUTE);
        publishNewState();
        // Do actual turning here
        TurnTableOrientation target = treq.getTto();
        context().system().scheduler().scheduleOnce(Duration.ofMillis(100), () -> turnTo(target), context().system().dispatcher());
        // checkTurningPositionReached(target); // this is called by TurnTo anyway
    }

    protected void completing() {
        tsm.fire(NEXT);
        publishNewState();       //we are now in COMPLETING
        complete();
    }

    protected void complete() {
        tsm.fire(NEXT);
        publishNewState();       //we are now in COMPLETE
        //autoResetToIdle(); // we dont want to reset immediately because that would mean we return to home immediately
    }

//    protected void autoResetToIdle() {
//        reset();
//    	//tsm.fire(NEXT);
//        //publishNewState();       //we are now in IDLE
//    }

    private void turnTo(TurnTableOrientation target) {
        log.info("Turning to target: " + target.toString());
        motorSetSpeed(200); //seems to be reset after resetting tacho count
        switch (target) {
            case NORTH:
                if (!sensorHomingHasDetectedInput()) {
                    //motorTurnTo(NORTH_ANGLE);
                    motorBackward();
                }
                checkTurningPositionReached(target);
                this.orientation = TurnTableOrientation.NORTH;
                log.info("Turned to North");
                break;
            case EAST:
                motorTurnTo(EAST_ANGLE);
                checkTurningPositionReached(target);
                log.info("Turned to East");
                this.orientation = TurnTableOrientation.EAST;
                break;
            case SOUTH:
                motorTurnTo(SOUTH_ANGLE);
                checkTurningPositionReached(target);
                log.info("Turned to South");
                this.orientation = TurnTableOrientation.SOUTH;
                break;
            case WEST:
                motorTurnTo(WEST_ANGLE);
                checkTurningPositionReached(target);
                log.info("Turned to West");
                this.orientation = TurnTableOrientation.WEST;
                break;
        }
        /*if (target.getNumericValue() > this.orientation.getNumericValue()) {
            turnRight(target);
        } else if (target.getNumericValue() < this.orientation.getNumericValue()) {
            turnLeft(target);
        }
        checkTurningPositionReached(target);*/
    }

   /* private void turnLeft(TurnTableOrientation target) {
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
    }*/

    /**
     * Turns right by the amount of degrees specified in rotationToNext
     */
   /* private void turnRight(TurnTableOrientation target) {
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
    }*/
    private void checkTurningPositionReached(TurnTableOrientation orientation) {
        if (this.tsm.isInState(TurningStates.EXECUTING) && this.orientation == orientation
                && isRotationFinished(orientation)) {
            log.info("Turning Position reached");
            if (orientation == TurnTableOrientation.NORTH) {
                resetTachoCount();
            }
            completing();
        } else {
            //TODO notify if stuck here and state is execute
            context().system().scheduler().scheduleOnce(Duration.ofMillis(100),
                    () -> checkTurningPositionReached(orientation)
                    , context().system().dispatcher());
        }
    }

    private boolean isRotationFinished(TurnTableOrientation orientation) {
        //log.info("Rotation is now:" + getPosition());
        switch (orientation) {
            case NORTH:
                return sensorHomingHasDetectedInput();    //Casting position from float to int somehow is always pos-1
            case EAST:
                return getPosition() >= EAST_ANGLE - 5;
            case SOUTH:
                return getPosition() >= SOUTH_ANGLE - 5;
            case WEST:
                return getPosition() >= WEST_ANGLE - 5;
            default:
                return false;
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

    private void motorTurnTo(int angle) {
        turningHardware.getTurningMotor().rotateTo(angle);
    }

    private void resetTachoCount() {
        turningHardware.getTurningMotor().resetTachoCount();
    }

    private void motorSetSpeed(int speed) {
        turningHardware.getTurningMotor().setSpeed(speed);
    }

    private int getPosition() {
        return turningHardware.getTurningMotor().getRotationAngle();
    }
}
