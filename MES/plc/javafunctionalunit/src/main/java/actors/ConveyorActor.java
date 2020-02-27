package actors;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.github.oxo42.stateless4j.StateMachine;
import event.bus.InterMachineEventBus;
import event.bus.StatePublisher;
import event.bus.WellknownMachinePropertyFields;
import hardware.ConveyorHardware;
import hardware.lego.LegoConveyorHardware;
import hardware.mock.ConveyorMockHardware;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import stateMachines.conveyor.ConveyorStateMachineConfig;
import stateMachines.conveyor.ConveyorStates;
import stateMachines.conveyor.ConveyorStatusUpdateEvent;
import stateMachines.conveyor.ConveyorTriggers;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static stateMachines.conveyor.ConveyorStates.STOPPED;

public class ConveyorActor extends AbstractActor {

    public static final boolean DEBUG = false;
    private AtomicBoolean stopped;

    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private InterMachineEventBus intraEventBus;

    private ConveyorHardware conveyorHardware;

    private StatePublisher publishEP;
    protected StateMachine<ConveyorStates, ConveyorTriggers> tsm;

    static public Props props(InterMachineEventBus intraEventBus, StatePublisher publishEP) {
        return Props.create(ConveyorActor.class, () -> new ConveyorActor(intraEventBus, publishEP));
    }

    public ConveyorActor(InterMachineEventBus intraEventBus, StatePublisher publishEP) {
        this.publishEP = publishEP;
        this.intraEventBus = intraEventBus;
        this.tsm = new StateMachine<>(STOPPED, new ConveyorStateMachineConfig());
        initHardware();
        stopped = new AtomicBoolean(true);
    }

    private void initHardware() {
        if(DEBUG) {
            conveyorHardware = new ConveyorMockHardware(200, 1000);
        }else{
            conveyorHardware = new LegoConveyorHardware(MotorPort.A, SensorPort.S2, SensorPort.S3);
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
            intraEventBus.publish(new ConveyorStatusUpdateEvent("", null, WellknownMachinePropertyFields.STATE_VAR_NAME, "", tsm.getState()));
        }
    }

    private void stop() {
        motorStop();
        context().system()
                .scheduler()
                .scheduleOnce(Duration.ofMillis(1000),
                        () -> {
                            stopped.set(true);
                            tsm.fire(ConveyorTriggers.NEXT);
                            publishNewState();
                        }, context().system().dispatcher());
    }

    private void reset() {
        motorStop();
        context().system()
                .scheduler()
                .scheduleOnce(Duration.ofMillis(1000),
                        () -> {
                            stopped.set(false);
                            tsm.fire(ConveyorTriggers.NEXT);
                            publishNewState();
                        }, context().system().dispatcher());
    }

    private void loadingToFullyOccupied() {
        motorBackward();
        checkForFullyLoaded();
        /*context().system()
                .scheduler()
                .scheduleOnce(Duration.ofMillis(2000),
                        () -> {
                            tsm.fire(ConveyorTriggers.NEXT);
                            publishNewState();                // now in fully occupied
                        }, context().system().dispatcher());*/
    }

    private void checkForFullyLoaded() {
        //if (isStopped()) return;
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
        /*context().system()
                .scheduler()
                .scheduleOnce(Duration.ofMillis(2000),
                        () -> {
                            tsm.fire(ConveyorTriggers.NEXT);
                            publishNewState();                // now in idle/empty
                        }, context().system().dispatcher());         */
    }

    private void checkForFullyUnloaded() {
        //if (isStopped()) return;
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

    private boolean isStopped() {
        return stopped.get();
    }
}
