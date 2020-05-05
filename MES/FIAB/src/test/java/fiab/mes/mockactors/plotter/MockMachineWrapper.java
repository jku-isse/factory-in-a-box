package fiab.mes.mockactors.plotter;

import java.time.Duration;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.plotting.PlotterMessageTypes;
import fiab.mes.eventbus.InterMachineEventBus;


public class MockMachineWrapper extends AbstractActor{

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	protected InterMachineEventBus interEventBus;
	protected BasicMachineStates currentState = BasicMachineStates.STOPPED;
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
				.match(PlotterMessageTypes.class, msg -> {
					switch(msg) {
					case SubscribeState: 
						doPublishState = true;
						setAndPublishState(currentState); //we publish the current state
						break;
					case Plot:
						if (currentState.equals(BasicMachineStates.IDLE))
							plot();
						else 
							log.warning("Wrapper told to plot in wrong state "+currentState);
						break;
					case Reset:
						if (currentState.equals(BasicMachineStates.STOPPED))
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
		setAndPublishState(BasicMachineStates.RESETTING);
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
		setAndPublishState(BasicMachineStates.STOPPING);
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
		setAndPublishState(BasicMachineStates.STARTING);
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
	
	protected void setAndPublishState(BasicMachineStates newState) {
		//log.debug(String.format("%s sets state from %s to %s", this.machineId.getId(), this.currentState, newState));
		this.currentState = newState;
		if (doPublishState) {
			interEventBus.publish(new MachineStatusUpdateEvent("", OPCUABasicMachineBrowsenames.STATE_VAR_NAME, "", newState));
		}
	}
	
	
	private void finishProduction() {
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(5000), 
    			 new Runnable() {
            @Override
            public void run() {
            	setAndPublishState(BasicMachineStates.COMPLETING); 
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
            	setAndPublishState(BasicMachineStates.IDLE); 
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
            	setAndPublishState(BasicMachineStates.STOPPED); 
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
            	setAndPublishState(BasicMachineStates.EXECUTE);
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
            	setAndPublishState(BasicMachineStates.COMPLETE); 
            	reset(); // we automatically reset
            }
          }, context().system().dispatcher());
	}
}
