package fiab.turntable.conveying;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.github.oxo42.stateless4j.StateMachine;

import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.StatePublisher;
import fiab.turntable.actor.IntraMachineEventBus;
import hardware.ConveyorHardware;
import hardware.lego.LegoConveyorHardware;
import hardware.mock.ConveyorMockHardware;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;

import static fiab.turntable.conveying.ConveyorStates.STOPPED;

import java.time.Duration;

public class ConveyorActor extends AbstractActor {

    //In case the operating system is windows, we do not want to use EV3 libraries
    private static final boolean DEBUG = System.getProperty("os.name").toLowerCase().contains("win");
    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private IntraMachineEventBus intraEventBus;

    private ConveyorHardware conveyorHardware;

    private StatePublisher publishEP;
    protected StateMachine<ConveyorStates, ConveyorTriggers> tsm;

    static public Props props(IntraMachineEventBus intraEventBus, StatePublisher publishEP) {
        return Props.create(ConveyorActor.class, () -> new ConveyorActor(intraEventBus, publishEP));
    }

    public ConveyorActor(IntraMachineEventBus intraEventBus, StatePublisher publishEP) {
        this.publishEP = publishEP;
        this.intraEventBus = intraEventBus;
        this.tsm = new StateMachine<>(STOPPED, new ConveyorStateMachineConfig());
        initHardware();
    }

    private void initHardware() {
        if (DEBUG) {
            conveyorHardware = new ConveyorMockHardware(200, 1000);
        } else {
            conveyorHardware = new LegoConveyorHardware(MotorPort.A, SensorPort.S2, SensorPort.S3);
            conveyorHardware.getConveyorMotor().setSpeed(500);
        }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ConveyorTriggers.class, trigger -> {
                    if (tsm.canFire(trigger)) {
                        switch (trigger) {
                            case LOAD:
                                tsm.fire(trigger);
                                publishNewState(); // loading now
                                loadingToFullyOccupied();
                                break;
                            case RESET:
                                tsm.fire(trigger);
                                publishNewState(); //now in resetting
                                reset();
                                break;
                            case STOP:
                                tsm.fire(ConveyorTriggers.STOP);
                                publishNewState(); // now in stopping
                                stop();
                                break;
                            case UNLOAD:
                                tsm.fire(trigger);
                                publishNewState(); // unloading now
                                unloadingToIdle();
                                break;
                            default: // all others are internal triggers
                                log.warning(String.format("Received internal transition trigger %s as an external request, ignoring", trigger));
                                break;
                        }
                    } else {
                        log.warning(String.format("Received request %s in unsuitable state %s", trigger, tsm.getState().toString()));
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
            intraEventBus.publish(new ConveyorStatusUpdateEvent("", OPCUABasicMachineBrowsenames.STATE_VAR_NAME, "", tsm.getState()));
        }
    }

    private void stop() {
        motorStop();
        tsm.fire(ConveyorTriggers.NEXT);
        publishNewState();
    }

    private void reset() {
        motorStop();
        tsm.fire(ConveyorTriggers.NEXT);
        publishNewState();
    }

    private void loadingToFullyOccupied() {
        motorBackward();
        checkForFullyLoaded();
    }

    private void checkForFullyLoaded() {
        if (sensorLoadingHasDetectedInput()) {
            motorStop();
            tsm.fire(ConveyorTriggers.NEXT);
            publishNewState();                // now in fully occupied
        } else {
            context().system()
                    .scheduler()
                    .scheduleOnce(Duration.ofMillis(100),
                            this::checkForFullyLoaded,
                            context().system().dispatcher());
        }
    }

    private void unloadingToIdle() {
        motorForward();
        checkForFullyUnloaded();
    }

    private void checkForFullyUnloaded() {
        if (!sensorUnloadingHasDetectedInput()) {
            motorStop();
            tsm.fire(ConveyorTriggers.NEXT);
            publishNewState();                // now in fully occupied
        } else {
            context().system()
                    .scheduler()
                    .scheduleOnce(Duration.ofMillis(100),
                            this::checkForFullyUnloaded,
                            context().system().dispatcher());
        }
    }

    private boolean sensorLoadingHasDetectedInput() {
        return conveyorHardware.getLoadingSensor().hasDetectedInput();
    }

    private boolean sensorUnloadingHasDetectedInput() {
        return conveyorHardware.getUnloadingSensor().hasDetectedInput();
    }

    private void motorForward() {
        conveyorHardware.getConveyorMotor().forward();
    }

    private void motorBackward() {
        conveyorHardware.getConveyorMotor().backward();
    }

    private void motorStop() {
        conveyorHardware.getConveyorMotor().stop();
    }
}
