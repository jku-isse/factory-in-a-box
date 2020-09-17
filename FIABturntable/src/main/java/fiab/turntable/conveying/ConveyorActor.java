package fiab.turntable.conveying;

import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.StatePublisher;
import fiab.turntable.actor.IntraMachineEventBus;
import fiab.turntable.conveying.statemachine.ConveyorTriggers;
import hardware.ConveyorHardware;
import hardware.lego.LegoConveyorHardware;
import hardware.mock.ConveyorMockHardware;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;

import java.time.Duration;

public class ConveyorActor extends BaseBehaviorConveyorActor {

    //In case the operating system is windows, we do not want to use EV3 libraries
    private static final boolean DEBUG = System.getProperty("os.name").toLowerCase().contains("win");
    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private ConveyorHardware conveyorHardware;

    static public Props props(IntraMachineEventBus intraEventBus, StatePublisher publishEP) {
        return Props.create(ConveyorActor.class, () -> new ConveyorActor(intraEventBus, publishEP));
    }

    public ConveyorActor(IntraMachineEventBus intraEventBus, StatePublisher publishEP) {
        super(intraEventBus, publishEP);    	
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

    private void publishNewState() {
        if (publishEP != null)
            publishEP.setStatusValue(tsm.getState().toString());
        if (intraEventBus != null) {
            intraEventBus.publish(new ConveyorStatusUpdateEvent("", OPCUABasicMachineBrowsenames.STATE_VAR_NAME, "", tsm.getState()));
        }
    }

    protected void stop() {
        motorStop();
        tsm.fire(ConveyorTriggers.NEXT);
        publishNewState();
    }

    protected void reset() {
        motorStop();
        tsm.fire(ConveyorTriggers.NEXT);
        publishNewState();
    }

    protected void loadingToFullyOccupied() {
        motorBackward();
        checkForFullyLoaded();
    }

    protected void checkForFullyLoaded() {
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

    protected void unloadingToIdle() {
        motorForward();
        checkForFullyUnloaded();
    }

    protected void checkForFullyUnloaded() {
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
