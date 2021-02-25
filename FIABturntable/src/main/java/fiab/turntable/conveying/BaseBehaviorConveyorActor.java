package fiab.turntable.conveying;

import static fiab.turntable.conveying.statemachine.ConveyorStates.STOPPED;

import java.time.Duration;

import com.github.oxo42.stateless4j.StateMachine;

import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.StatePublisher;
import fiab.tracing.actor.AbstractTracingActor;
import fiab.turntable.actor.IntraMachineEventBus;
import fiab.turntable.actor.messages.ConveyorTriggerMessage;
import fiab.turntable.conveying.statemachine.ConveyorStateMachineConfig;
import fiab.turntable.conveying.statemachine.ConveyorStates;
import fiab.turntable.conveying.statemachine.ConveyorTriggers;

public class BaseBehaviorConveyorActor extends AbstractTracingActor {

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
        		.match(ConveyorTriggerMessage.class, msg->{
        			receiveConveyorTrigger(msg);
        			
        		})
                .match(ConveyorTriggers.class, trigger -> {
                	receiveConveyorTrigger(new ConveyorTriggerMessage("", trigger));
                   
                })
                .matchAny(msg -> {
                    log.warning("Unexpected Message received: " + msg.toString());
                })
                .build();
    }

    private void receiveConveyorTrigger(ConveyorTriggerMessage msg) {
    	ConveyorTriggers trigger = msg.getBody();
    	try {
    		tracingFactory.startConsumerSpan(msg, "Conveyor Actor: Trigger "+trigger.toString()+" received"); 		
    		
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
			
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			tracingFactory.finishCurrentSpan();
		}
    	
    	
		
	}

	private void publishNewState() {
        if (publishEP != null)
            publishEP.setStatusValue(tsm.getState().toString());
        if (intraEventBus != null) {
        	ConveyorStatusUpdateEvent event = new ConveyorStatusUpdateEvent("", OPCUABasicMachineBrowsenames.STATE_VAR_NAME, "", tsm.getState());
        	event.setHeader(tracingFactory.getCurrentHeader());
        	tracingFactory.injectMsg(event);
        	
            intraEventBus.publish(event);
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
