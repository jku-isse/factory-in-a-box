package actors;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.github.oxo42.stateless4j.StateMachine;
import event.TurntableStatusUpdateEvent;
import event.bus.InterMachineEventBus;
import event.bus.StatePublisher;
import event.bus.WellknownMachinePropertyFields;
import hardware.TurningHardware;
import hardware.lego.LegoTurningHardware;
import hardware.mock.TurningMockHardware;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import msg.GenericMachineRequests;
import msg.TurnRequest;
import stateMachines.turning.TurnTableOrientation;
import stateMachines.turning.TurningStateMachineConfig;
import stateMachines.turning.TurningStates;
import stateMachines.turning.TurningTriggers;

import java.time.Duration;

import static stateMachines.turning.TurningStates.STOPPED;
import static stateMachines.turning.TurningTriggers.*;

public class TurntableActor extends AbstractActor {

    public static final boolean DEBUG = false;
    private final int timeForNinetyDeg = 1350;
    private boolean stopped;

    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private TurningHardware turningHardware;

    private InterMachineEventBus intraEventBus;
    private StatePublisher publishEP;

    protected TurnTableOrientation orientation;
    protected StateMachine<TurningStates, TurningTriggers> tsm;

    public static Props props(InterMachineEventBus intraEventBus, StatePublisher publishEP) {
        return Props.create(TurntableActor.class, () -> new TurntableActor(intraEventBus, publishEP));
    }

    public TurntableActor(InterMachineEventBus intraEventBus, StatePublisher publishEP) {
        this.intraEventBus = intraEventBus;
        this.publishEP = publishEP;
        this.tsm = new StateMachine<>(STOPPED, new TurningStateMachineConfig());
        this.orientation = TurnTableOrientation.NORTH;
        this.stopped = true;
        Runtime.getRuntime().addShutdownHook(new Thread(this::motorStop));
        initHardware();
        publishNewState();
    }

    private void initHardware() {
        if(DEBUG) {
            turningHardware = new TurningMockHardware(200);
        }else{
            turningHardware = new LegoTurningHardware(MotorPort.D, SensorPort.S4);
        }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(GenericMachineRequests.Stop.class, req -> {
                    if (tsm.canFire(STOP)) {
                        tsm.fire(STOP);
                        publishNewState();  //in STOPPING
                        stop();
                    }
                }).match(GenericMachineRequests.Reset.class, req -> {
                    if (tsm.canFire(RESET)) {
                        tsm.fire(RESET);    //in RESETTING
                        publishNewState();
                        reset();
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
            intraEventBus.publish(new TurntableStatusUpdateEvent("", null, WellknownMachinePropertyFields.STATE_VAR_NAME, "", tsm.getState()));
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
        /*while (!turningMockHardware.getMockSensorHoming().hasDetectedInput()) {
            if (stopped) {
                return;
            }
        }
        context().system()
                .scheduler()
                .scheduleOnce(Duration.ofMillis(1000),
                        new Runnable() {
                            @Override
                            public void run() {
                                stopped = false;
                                tsm.fire(NEXT);
                                publishNewState();
                            }
                        }, context().system().dispatcher());*/
    }

    /**
     * Checks periodically whether homing position is reached. If reached, machine goes idle
     */
    private void checkHomingPositionReached() {
        //if (stopped) return;
        if (sensorHomingHasDetectedInput()) {
            motorStop();
            stopped = false;
            tsm.fire(NEXT);
            publishNewState();
        } else {
            context().system().scheduler().scheduleOnce(Duration.ofMillis(100),
                    this::checkHomingPositionReached
                    , context().system().dispatcher());
        }
    }

    private void turn(TurnRequest treq) {
        context().system()
                .scheduler()
                .scheduleOnce(Duration.ofMillis(1000),
                        () -> {
                            tsm.fire(EXECUTE);
                            // Do actual turning here
                            turnTo(treq.getTto());
                            publishNewState();
                            completing();
                        }, context().system().dispatcher());
    }

    private void completing() {
        context().system()
                .scheduler()
                .scheduleOnce(Duration.ofMillis(1000),
                        () -> {
                            tsm.fire(NEXT);
                            publishNewState();       //we are now in COMPLETING
                            complete();
                        }, context().system().dispatcher());
    }

    private void complete() {
        context().system()
                .scheduler()
                .scheduleOnce(Duration.ofMillis(1000),
                        () -> {
                            tsm.fire(NEXT);
                            publishNewState();       //we are now in COMPLETE
                            autoResetToIdle();
                        }, context().system().dispatcher());
    }

    private void autoResetToIdle() {
        context().system()
                .scheduler()
                .scheduleOnce(Duration.ofMillis(1000),
                        () -> {
                            tsm.fire(NEXT);
                            publishNewState();       //we are now in IDLE
                        }, context().system().dispatcher());
    }

    private void turnTo(TurnTableOrientation target) {
        if (target.getNumericValue() > this.orientation.getNumericValue()) {
            while (!(target.getNumericValue() == this.orientation.getNumericValue())) {
                if (stopped) {
                    return;
                }
                turnRight();
            }
        } else {
            while (!(target.getNumericValue() == this.orientation.getNumericValue())) {
                if (stopped) {
                    return;
                }
                turnLeft();
            }
        }
    }

    private void turnLeft() {
        if (this.orientation == TurnTableOrientation.NORTH) {
            System.out.println("Cannot turn left from North");
        }
        System.out.println("Executing from turning: turnLeft");
        motorBackward();
        context().system().scheduler().scheduleOnce(Duration.ofMillis(timeForNinetyDeg),
                this::motorStop,
                context().system().dispatcher());
        //this.turnMotor.rotate(-rotationToNext);
        orientation = orientation.getNextCounterClockwise(orientation);
        System.out.println("Orientation is now: " + orientation);
    }

    /**
     * Turns right by the amount of degrees specified in rotationToNext
     */
    private void turnRight() {
        if (this.orientation == TurnTableOrientation.WEST) {
            System.out.println("Cannot turn right from West");
            return;
        }
        System.out.println("Executing from turning: turnRight");
        motorForward();
        context().system().scheduler().scheduleOnce(Duration.ofMillis(timeForNinetyDeg),
                this::motorStop,
                context().system().dispatcher());
        orientation = orientation.getNextClockwise(orientation);
        System.out.println("Orientation is now: " + orientation);
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
