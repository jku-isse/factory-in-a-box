package fiab.mes.mockactors;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.mes.transport.handshake.HandshakeProtocol;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerSide;
import fiab.opcua.hardwaremock.StatePublisher;


public class MockServerHandshakeActor extends AbstractActor{

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);	
	protected boolean isLoaded = false; //assume at bootup that no pallet is loaded
	private ActorRef machineWrapper;
	protected ServerSide currentState = ServerSide.STOPPED;
	//protected ActorRef clientSide;
	protected ActorRef self;
	protected Set<ActorRef> subscribers = new HashSet<ActorRef>();
	protected boolean doAutoComplete = false;
	protected StatePublisher publishEP;
	
	static public Props props(ActorRef machineWrapper, boolean doAutoComplete) {	    
		return Props.create(MockServerHandshakeActor.class, () -> new MockServerHandshakeActor(machineWrapper, doAutoComplete, null));
	}
	
	static public Props props(ActorRef machineWrapper, boolean doAutoComplete, StatePublisher publishEP) {	    
		return Props.create(MockServerHandshakeActor.class, () -> new MockServerHandshakeActor(machineWrapper, doAutoComplete, publishEP));
	}
	
	public MockServerHandshakeActor(ActorRef machineWrapper, boolean doAutoComplete, StatePublisher publishEP) {
		this.machineWrapper = machineWrapper;
		this.doAutoComplete = doAutoComplete;
		this.publishEP = publishEP;
		self = getSelf();
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(HandshakeProtocol.ServerMessageTypes.class, msg -> {
					log.info(String.format("Received %s from %s", msg, getSender()));
					switch(msg) {					
					case Complete:
						if (currentState.equals(ServerSide.EXECUTE)) {
							complete();
						}
						break;
					case RequestInitiateHandover:					
						initHandover();
						break;
					case RequestStartHandover:
						startHandover();
						break;
					case Reset:
						if (currentState.equals(ServerSide.STOPPED) || currentState.equals(ServerSide.COMPLETE)) {
							reset();
						}
						break;
					case Stop:
						stop();
						break;
					case SubscribeToStateUpdates:
						subscribers.add(getSender());
						getSender().tell(currentState, getSelf()); // update subscriber with current state
						break;
					case UnsubscribeToStateUpdates:
						subscribers.remove(getSender());
						break;
					default:
						break;
					}
				})
				.match(StateOverrideRequests.class, req -> {
					log.info(String.format("Received %s from %s", req, getSender()));
					switch(req) {
					case SetLoaded:
						updateLoadState(true);
						break;
					case SetEmpty:
						updateLoadState(false);
						break;
					default:
						break;
					}
				})
				.matchAny(msg -> { 
					log.warning(String.format("Unexpected Message received <%s> from %s", msg.toString(), getSender() )); })
		        .build();
	}

	protected void publishNewState(ServerSide newState) {
		currentState = newState;
		if (machineWrapper != null) {
			machineWrapper.tell(newState, self);
		}
		if (publishEP != null) {
			publishEP.setStatusValue(newState.toString());
		}
		ImmutableSet.copyOf(subscribers).stream().forEach(sub -> sub.tell(newState, self));
	}
	
	protected void updateLoadState(boolean isLoaded) {
		if (this.isLoaded != isLoaded) {
			this.isLoaded = isLoaded;
			if (currentState != ServerSide.STOPPED)
				stop();
		}
	}
	
	protected void reset() {;
		publishNewState(ServerSide.RESETTING);
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000), 
    			 new Runnable() {
            @Override
            public void run() {
            	if (isLoaded) {
            		publishNewState(ServerSide.IDLE_LOADED);
            	} else {
            		publishNewState(ServerSide.IDLE_EMPTY);
            	}
            }
          }, context().system().dispatcher());
	}	
	
	private void initHandover() {
		if (currentState.equals(ServerSide.IDLE_EMPTY) || currentState.equals(ServerSide.IDLE_LOADED)) {
			publishNewState(ServerSide.STARTING);			
			log.info(String.format("Responding with OkResponseInitHandover to %s", getSender()));
			getSender().tell(HandshakeProtocol.ServerMessageTypes.OkResponseInitHandover, self);
			context().system()
	    	.scheduler()
	    	.scheduleOnce(Duration.ofMillis(200), 
	    			 new Runnable() {
	            @Override
	            public void run() {
	            	publishNewState(ServerSide.PREPARING);
	            	if (isLoaded) {	            		
	            		publishNewState(ServerSide.READY_LOADED);
	            	} else {	            		
	            		publishNewState(ServerSide.READY_EMPTY);
	            	}
	            }
	          }, context().system().dispatcher());
		} else if (currentState.equals(ServerSide.READY_EMPTY) || currentState.equals(ServerSide.READY_LOADED)){
			getSender().tell(HandshakeProtocol.ServerMessageTypes.OkResponseInitHandover, self); // resending
		} else {
			log.warning(String.format("Responding with NotOkResponseInitHandover to %s in state %s", getSender(), currentState));
			getSender().tell(HandshakeProtocol.ServerMessageTypes.NotOkResponseInitHandover, self);
		}
		
	} 		
	
	private void  startHandover() {
		if ((currentState.equals(ServerSide.READY_EMPTY) || currentState.equals(ServerSide.READY_LOADED))) {
			publishNewState(ServerSide.EXECUTE);
			log.info(String.format("Responding with OkResponseStartHandover to %s", getSender()));
			getSender().tell(HandshakeProtocol.ServerMessageTypes.OkResponseStartHandover, self);
		} else if ( currentState.equals(ServerSide.EXECUTE) ){ //resending
			getSender().tell(HandshakeProtocol.ServerMessageTypes.OkResponseStartHandover, self);
		} else {
			log.warning(String.format("Responding with NotOkResponseStartHandover to %s in state %s", getSender(), currentState));
			getSender().tell(HandshakeProtocol.ServerMessageTypes.NotOkResponseStartHandover, self);
		}	
		if (doAutoComplete) {
			context().system()
			.scheduler()
			.scheduleOnce(Duration.ofMillis(1000), 
					new Runnable() {
				@Override
				public void run() {
					complete();
				}
			}, context().system().dispatcher());
		}
	}
	
	private void complete() {
		publishNewState(ServerSide.COMPLETING);
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000), 
    			 new Runnable() {
            @Override
            public void run() {
            	// we toggle load flag as when we were empty now we are loaded and vice versa
            	isLoaded = !isLoaded;
            	publishNewState(ServerSide.COMPLETE); 
//            	stop(); // we automatically stop --> no longer stop, just remain in complete
            }
          }, context().system().dispatcher());
	}			
	
	private void stop() {
		publishNewState(ServerSide.STOPPING);
		//clientSide = null;
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000), 
    			 new Runnable() {
            @Override
            public void run() {
            	publishNewState(ServerSide.STOPPED);
            }
          }, context().system().dispatcher());
	}	
	
	public static enum StateOverrideRequests {
		SetLoaded, SetEmpty
	}
}
