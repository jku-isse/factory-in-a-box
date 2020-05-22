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
import fiab.turntable.conveying.BaseBehaviorConveyorActor;
import hardware.TurningHardware;
import hardware.lego.LegoTurningHardware;
import hardware.mock.TurningMockHardware;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;

import static fiab.turntable.turning.TurningStates.STOPPED;
import static fiab.turntable.turning.TurningTriggers.*;

import java.time.Duration;

public class BaseBehaviorTurntableActor extends AbstractActor {
  
    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    
    protected IntraMachineEventBus intraEventBus;
    protected StatePublisher publishEP;

    protected StateMachine<TurningStates, TurningTriggers> tsm;

    public static Props props(IntraMachineEventBus intraEventBus, StatePublisher publishEP) {
        return Props.create(BaseBehaviorTurntableActor.class, () -> new BaseBehaviorTurntableActor(intraEventBus, publishEP));
    }

    public BaseBehaviorTurntableActor(IntraMachineEventBus intraEventBus, StatePublisher publishEP) {
        this.intraEventBus = intraEventBus;
        this.publishEP = publishEP;
        this.tsm = new StateMachine<>(STOPPED, new TurningStateMachineConfig());
        publishNewState();
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

    protected void publishNewState() {
        if (publishEP != null)
            publishEP.setStatusValue(tsm.getState().toString());
        if (intraEventBus != null) {
            intraEventBus.publish(new TurntableStatusUpdateEvent("", OPCUABasicMachineBrowsenames.STATE_VAR_NAME, "", tsm.getState()));
        }
    }

    protected void stop() {
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000), 
    			 new Runnable() {
            @Override
            public void run() {
            	tsm.fire(TurningTriggers.NEXT);
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
            	tsm.fire(TurningTriggers.NEXT);
            	publishNewState();            	
            }
          }, context().system().dispatcher());
	}
	
    protected void turn(TurnRequest treq) {		
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000), 
    			 new Runnable() {
            @Override
            public void run() {
            	tsm.fire(TurningTriggers.EXECUTE);
            	// Do actual turning here
            	publishNewState();       
            	completing();
            }
          }, context().system().dispatcher());
	}
	
    protected void completing() {		
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000), 
    			 new Runnable() {
            @Override
            public void run() {
            	tsm.fire(TurningTriggers.NEXT);            	
            	publishNewState();       //we are now in COMPETING
            	complete();
            }
          }, context().system().dispatcher());
	}
	
    protected void complete() {		
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000), 
    			 new Runnable() {
            @Override
            public void run() {
            	tsm.fire(TurningTriggers.NEXT);            	
            	publishNewState();       //we are now in COMPETE
            	autoResetToIdle();
            }
          }, context().system().dispatcher());
	}
	
    protected void autoResetToIdle() {		
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000), 
    			 new Runnable() {
            @Override
            public void run() {
            	tsm.fire(TurningTriggers.NEXT);            	
            	publishNewState();       //we are now in IDLE            	
            }
          }, context().system().dispatcher());
	}

   
}
