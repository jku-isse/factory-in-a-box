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

public class BaseBehaviorConveyorActor extends AbstractActor {

    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    protected IntraMachineEventBus intraEventBus;

    protected StatePublisher publishEP;
    protected StateMachine<ConveyorStates, ConveyorTriggers> tsm;

    static public Props props(IntraMachineEventBus intraEventBus, StatePublisher publishEP) {
        return Props.create(BaseBehaviorConveyorActor.class, () -> new BaseBehaviorConveyorActor(intraEventBus, publishEP));
    }

    public BaseBehaviorConveyorActor(IntraMachineEventBus intraEventBus, StatePublisher publishEP) {
        this.publishEP = publishEP;
        this.intraEventBus = intraEventBus;
        this.tsm = new StateMachine<>(STOPPED, new ConveyorStateMachineConfig());    
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

    protected void stop() {
    	context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000), 
    			 new Runnable() {
            @Override
            public void run() {
            	tsm.fire(ConveyorTriggers.NEXT);
            	publishNewState();
            }
          }, context().system().dispatcher());
    }

    protected void reset() {
    	context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000), 
    			 new Runnable() {
            @Override
            public void run() {
            	tsm.fire(ConveyorTriggers.NEXT);
            	publishNewState();            	
            }
          }, context().system().dispatcher());
    }

    protected void loadingToFullyOccupied() {
		context().system()
    	.scheduler()
		.scheduleOnce(Duration.ofMillis(2000), 
    			 new Runnable() {
            @Override
            public void run() {
            	tsm.fire(ConveyorTriggers.NEXT); 
            	publishNewState();            	// now in fully occupied
            }
          }, context().system().dispatcher());
    }
    
    protected void unloadingToIdle() {
    	context().system()
    	.scheduler()
		.scheduleOnce(Duration.ofMillis(2000), 
    			 new Runnable() {
            @Override
            public void run() {
            	tsm.fire(ConveyorTriggers.NEXT); 
            	publishNewState();            	// now in idle/empty
            }
          }, context().system().dispatcher());
    }




}
