package fiab.mes.mockactors.transport;

import java.time.Duration;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.machine.actor.WellknownMachinePropertyFields;
import fiab.mes.machine.msg.MachineStatus;
import fiab.mes.machine.msg.MachineStatusUpdateEvent;
import fiab.mes.mockactors.MockServerHandshakeActor;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerSide;
import fiab.mes.transport.msg.TransportModuleRequest;

public class MockTransportModuleWrapper extends AbstractActor{

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	protected InterMachineEventBus interEventBus;
	protected boolean doPublishState = false;
	protected ActorRef self;
	protected MachineStatus currentState = MachineStatus.STOPPED;
	
	static public Props props(InterMachineEventBus internalMachineEventBus) {	    
		return Props.create(MockTransportModuleWrapper.class, () -> new MockTransportModuleWrapper(internalMachineEventBus));
	}
	
	public MockTransportModuleWrapper(InterMachineEventBus machineEventBus) {
		this.interEventBus = machineEventBus;
		self = getSelf();
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(SimpleMessageTypes.class, msg -> {
					switch(msg) {
					case Reset:
						if (currentState.equals(MachineStatus.STOPPED))
							reset();
						else 
							log.warning("Wrapper told to reset in wrong state "+currentState);
						break;
					case Stop:
						stop();
						break;
					case SubscribeState:
						doPublishState = true;
						break;
					default:
						break;
					}
				})
				.match(TransportModuleRequest.class, req -> {
					if (currentState.equals(MachineStatus.IDLE)) {
		        		startTransport(req);
					} else {
		        		//TODO: respond with error message that we are not in the right state for request
		        	}
				})
				// TODO: all events coming from handshakeFUs
				.build();
	}

	protected void setAndPublishState(MachineStatus newState) {
		//log.debug(String.format("%s sets state from %s to %s", this.machineId.getId(), this.currentState, newState));
		this.currentState = newState;
		if (doPublishState) {
			interEventBus.publish(new MachineStatusUpdateEvent("", null, WellknownMachinePropertyFields.STATE_VAR_NAME, "", newState));
		}
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
	
	private void startTransport(TransportModuleRequest req) {
		setAndPublishState(MachineStatus.STARTING);
		// check which two handshake FUs we use,
		// transition into EXECUTE
		// imitate turning towards first
		// reset the first,
		// when execute, immitate loading, complete first
		// imitate turning towards second
				// reset the second,
				// when execute, immitate loading, complete second
		// transition into Completing
	}
	
	private void stop() {
		setAndPublishState(MachineStatus.STOPPING);
		//TODO tell all handshake FUs to stop, we ignore HandshakeFU level for now
		// serverSide.tell(MockServerHandshakeActor.MessageTypes.Stop, getSelf());
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000), 
    			 new Runnable() {
            @Override
            public void run() {
            	// only when handshakeFU and other FUs have stopped
            	//if (handshakeStatus.equals(ServerSide.Stopped)) {
            		transitionToStop();
            	//}
            }
          }, context().system().dispatcher());
	}
	
	private void transitionToStop() {
		setAndPublishState(MachineStatus.STOPPED); 
	}
	
	public static enum SimpleMessageTypes {
		SubscribeState, Reset, Stop
	}
}
