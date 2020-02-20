package fiab.mes.mockactors.transport.FUs;

import java.time.Duration;

import com.github.oxo42.stateless4j.StateMachine;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.machine.actor.WellknownMachinePropertyFields;
import fiab.mes.machine.msg.GenericMachineRequests;
import fiab.opcua.hardwaremock.StatePublisher;
import stateMachines.turning.TurnRequest;
import stateMachines.turning.TurningStateMachineConfig;
import stateMachines.turning.TurningStates;
import stateMachines.turning.TurningTriggers;
import stateMachines.turning.TurntableStatusUpdateEvent;

import static stateMachines.turning.TurningStates.*;

public class MockTurntableActor extends AbstractActor{

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);	
	
	private InterMachineEventBus intraEventBus;
	
	private StatePublisher publishEP;
	protected StateMachine<TurningStates, TurningTriggers> tsm;
	
	static public Props props(InterMachineEventBus intraEventBus, StatePublisher publishEP) {	    
		return Props.create(MockTurntableActor.class, () -> new MockTurntableActor(intraEventBus, publishEP));
	}
	
	public MockTurntableActor(InterMachineEventBus intraEventBus, StatePublisher publishEP) {
		this.publishEP = publishEP;
		this.intraEventBus = intraEventBus;
		this.tsm = new StateMachine<>(STOPPED, new TurningStateMachineConfig());
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()	
				.match(GenericMachineRequests.Stop.class, req -> {
					if (tsm.canFire(TurningTriggers.STOP)) {
						tsm.fire(TurningTriggers.STOP);
						stop();
					}
				})
				.match(GenericMachineRequests.Reset.class, req -> {
					if (tsm.canFire(TurningTriggers.RESET)) {
						tsm.fire(TurningTriggers.RESET);
						reset();
					}
				})
				.match(TurnRequest.class, req -> {
					if (tsm.canFire(TurningTriggers.TURN_TO)) {
						tsm.fire(TurningTriggers.TURN_TO);
						turn(req);
					}
				})
				.matchAny(msg -> { 
					log.warning("Unexpected Message received: "+msg.toString()); })
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
	
	private void reset() {		
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
	
	private void turn(TurnRequest treq) {		
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
	
	private void completing() {		
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
	
	private void complete() {		
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
	
	private void autoResetToIdle() {		
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
