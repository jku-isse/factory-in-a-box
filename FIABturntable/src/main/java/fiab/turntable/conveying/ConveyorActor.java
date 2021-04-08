package fiab.turntable.conveying;

import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.StatePublisher;
import fiab.turntable.actor.IntraMachineEventBus;
import fiab.turntable.conveying.statemachine.ConveyorTriggers;
import hardware.ConveyorHardware;
import config.HardwareInfo;

import java.time.Duration;

public class ConveyorActor extends BaseBehaviorConveyorActor {

    //In case the operating system is windows, we do not want to use EV3 libraries
    private static final boolean DEBUG = System.getProperty("os.name").toLowerCase().contains("win");
    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private ConveyorHardware conveyorHardware;

    static public Props props(IntraMachineEventBus intraEventBus, StatePublisher publishEP, HardwareInfo hardwareInfo) {
        return Props.create(ConveyorActor.class, () -> new ConveyorActor(intraEventBus, publishEP, hardwareInfo));
    }

    public ConveyorActor(IntraMachineEventBus intraEventBus, StatePublisher publishEP, HardwareInfo hardwareInfo) {
        super(intraEventBus, publishEP);
        initHardware(hardwareInfo);
    }

    private void initHardware(HardwareInfo hardwareInfo) {
        /*if (DEBUG) {
            if (hardwareConfig.getMotorA().isPresent() && hardwareConfig.getSensor2().isPresent() && hardwareConfig.getSensor3().isPresent()) {
                conveyorHardware = new ConveyorMockHardware(hardwareConfig.getMotorA().get(), hardwareConfig.getSensor2().get(), hardwareConfig.getSensor3().get());
            }//new ConveyorMockHardware(200, 1000);
        } else {
            conveyorHardware = new LegoConveyorHardware(MotorPort.A, SensorPort.S2, SensorPort.S3); //Use config
            conveyorHardware.getConveyorMotor().setSpeed(500);
        }*/
        if(hardwareInfo.getConveyorHardware().isPresent()){
            conveyorHardware = hardwareInfo.getConveyorHardware().get();
        }else{
            throw new RuntimeException("ConveyorHardware could not be initialized");
        }
    }


    private void publishNewState() {
        if (publishEP != null)
            publishEP.setStatusValue(tsm.getState().toString());
        if (intraEventBus != null) {
        	ConveyorStatusUpdateEvent event = new ConveyorStatusUpdateEvent("", OPCUABasicMachineBrowsenames.STATE_VAR_NAME, "", tsm.getState());
        	event.setTracingHeader(tracer.getCurrentHeader());
        	tracer.injectMsg(event);        	
        	
            intraEventBus.publish(event);
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
