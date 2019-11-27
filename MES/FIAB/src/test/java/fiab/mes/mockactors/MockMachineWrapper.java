package fiab.mes.mockactors;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ActorCoreModel.Actor;
import ProcessCore.AbstractCapability;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.actor.WellknownMachinePropertyFields;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.machine.msg.MachineEvent;
import fiab.mes.machine.msg.MachineStatus;
import fiab.mes.machine.msg.MachineStatusUpdateEvent;
import fiab.mes.machine.msg.MachineUpdateEvent;
import fiab.mes.order.msg.LockForOrder;
import fiab.mes.order.msg.ReadyForProcessEvent;
import fiab.mes.order.msg.RegisterProcessStepRequest;
import fiab.mes.planer.actor.MachineOrderMappingManager;


public class MockMachineWrapper extends AbstractActor{

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	protected InterMachineEventBus interEventBus;
	protected MachineStatus currentState = MachineStatus.STOPPED;
	protected boolean doPublishState = false;
	
	
	static public Props props(InterMachineEventBus internalMachineEventBus) {	    
		return Props.create(MockMachineWrapper.class, () -> new MockMachineWrapper(internalMachineEventBus));
	}
	
	public MockMachineWrapper(InterMachineEventBus machineEventBus) {
		this.interEventBus = machineEventBus;
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(MessageTypes.class, msg -> {
					switch(msg) {
					case SubscribeState: 
						doPublishState = true;
						setAndPublishState(currentState); //we publish the current state
						break;
					case Plot:
						if (currentState.equals(MachineStatus.IDLE))
							plot();
						else 
							log.warning("Wrapper told to plot in wrong state "+currentState);
						break;
					case Reset:
						if (currentState.equals(MachineStatus.STOPPED))
							reset();
						else 
							log.warning("Wrapper told to reset in wrong state "+currentState);
						break;
					case Stop:
						stop();
						break;
					default:
						break;
					}
				})
				.matchAny(msg -> { 
					log.warning("Unexpected Message received: "+msg.toString()); })
		        .build();
	}

	private void reset() {
		setAndPublishState(MachineStatus.RESETTING);
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000), 
    			 new Runnable() {
            @Override
            public void run() {
            	 transitionResettingToIdle();
            }
          }, context().system().dispatcher());
	}
	
	private void stop() {
		setAndPublishState(MachineStatus.STOPPING);
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000), 
    			 new Runnable() {
            @Override
            public void run() {
            	 transitionStoppingToStop();
            }
          }, context().system().dispatcher());
	}
	
	private void plot() {
		setAndPublishState(MachineStatus.STARTING);
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000), 
    			 new Runnable() {
            @Override
            public void run() {
            	 transitionStartingToExecute();
            }
          }, context().system().dispatcher());
	} 
	
	protected void setAndPublishState(MachineStatus newState) {
		//log.debug(String.format("%s sets state from %s to %s", this.machineId.getId(), this.currentState, newState));
		this.currentState = newState;
		if (doPublishState) {
			interEventBus.publish(new MachineStatusUpdateEvent("", null, WellknownMachinePropertyFields.STATE_VAR_NAME, "", newState));
		}
	}
	
	
	private void finishProduction() {
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(5000), 
    			 new Runnable() {
            @Override
            public void run() {
            	setAndPublishState(MachineStatus.COMPLETING); 
            	transitionCompletingToComplete();
            }
          }, context().system().dispatcher());
	}
	
	private void transitionResettingToIdle() {
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000), 
    			 new Runnable() {
            @Override
            public void run() {
            	setAndPublishState(MachineStatus.IDLE); 
            }
          }, context().system().dispatcher());
	}
	
	private void transitionStoppingToStop() {
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000), 
    			 new Runnable() {
            @Override
            public void run() {
            	setAndPublishState(MachineStatus.STOPPED); 
            }
          }, context().system().dispatcher());
	}
	
	private void transitionStartingToExecute() {
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(2000), 
    			 new Runnable() {
            @Override
            public void run() {
            	setAndPublishState(MachineStatus.EXECUTE);
            	finishProduction();
            }
          }, context().system().dispatcher());
	}
	
	private void transitionCompletingToComplete() {
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000), 
    			 new Runnable() {
            @Override
            public void run() {
            	setAndPublishState(MachineStatus.COMPLETE); 
            	reset(); // we automatically reset
            }
          }, context().system().dispatcher());
	}
	
	
	public static enum MessageTypes {
		SubscribeState, Reset, Plot, Stop
	}
}
