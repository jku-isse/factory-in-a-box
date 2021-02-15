package fiab.handshake.actor;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Sets;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.core.capabilities.handshake.IOStationCapability;
import fiab.core.capabilities.StatePublisher;
import fiab.core.capabilities.handshake.HandshakeCapability.ServerSideStates;
import fiab.core.capabilities.handshake.HandshakeCapability.StateOverrideRequests;


public class ServerSideHandshakeActor extends AbstractActor{

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);	
	protected boolean isLoaded = false; //assume at bootup that no pallet is loaded
	private ActorRef parentActor;
	protected ServerSideStates currentState = ServerSideStates.STOPPED;
	//protected ActorRef clientSide;
	protected ActorRef self;
	protected Set<ActorRef> subscribers = new HashSet<ActorRef>();
	protected boolean doAutoComplete = false;
	protected StatePublisher publishEP;
	
	static public Props props(ActorRef parent, boolean doAutoComplete) {	    
		return Props.create(ServerSideHandshakeActor.class, () -> new ServerSideHandshakeActor(parent, doAutoComplete, null));
	}
	
	static public Props props(ActorRef parent, boolean doAutoComplete, StatePublisher publishEP) {	    
		return Props.create(ServerSideHandshakeActor.class, () -> new ServerSideHandshakeActor(parent, doAutoComplete, publishEP));
	}
	
	public ServerSideHandshakeActor(ActorRef machineWrapper, boolean doAutoComplete, StatePublisher publishEP) {
		this.parentActor = machineWrapper;
		this.doAutoComplete = doAutoComplete;
		this.publishEP = publishEP;
		self = getSelf();
		publishNewState(currentState);
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(IOStationCapability.ServerMessageTypes.class, msg -> {
					log.info(String.format("Received %s from %s", msg, getSender()));
					switch(msg) {					
					case Complete:
						if (currentState.equals(ServerSideStates.EXECUTE)) {
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
						if (currentState.equals(ServerSideStates.STOPPED) || currentState.equals(ServerSideStates.COMPLETE)) {
							reset();
						}
						break;
					case Stop:
						stop();
						break;
					case SubscribeToStateUpdates:
						if (getSender() != context().system().deadLetters()) {
							subscribers.add(getSender());
							getSender().tell(currentState, getSelf()); // update subscriber with current state
						} else {
							publishNewState(currentState);
						}
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

	protected void publishNewState(ServerSideStates newState) {
		currentState = newState;
		if (parentActor != null) {
			parentActor.tell(newState, self);
		}
		if (publishEP != null) {
			publishEP.setStatusValue(newState.toString());
		}
		subscribers.stream().forEach(sub -> sub.tell(newState, self));
	}
	
	private Set<ServerSideStates> loadChangeableStates = Sets.newHashSet(ServerSideStates.COMPLETE, ServerSideStates.COMPLETING, ServerSideStates.STOPPED, ServerSideStates.STOPPING);
	
	protected boolean updateLoadState(boolean isLoaded) {
//		if (this.isLoaded != isLoaded) {
//			this.isLoaded = isLoaded;
//			if (currentState != ServerSideStates.STOPPED)
//				stop();
//		}
		// allow only when in stopped:
		if (loadChangeableStates.contains(currentState)) {
			this.isLoaded = isLoaded;
			return true;
		}
		return false;
	}
	
	protected void reset() {;
		publishNewState(ServerSideStates.RESETTING);
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000), 
    			 new Runnable() {
            @Override
            public void run() {
            	if (isLoaded) {
            		publishNewState(ServerSideStates.IDLE_LOADED);
            	} else {
            		publishNewState(ServerSideStates.IDLE_EMPTY);
            	}
            }
          }, context().system().dispatcher());
	}	
	
	private void initHandover() {
		if (currentState.equals(ServerSideStates.IDLE_EMPTY) || currentState.equals(ServerSideStates.IDLE_LOADED)) {
			publishNewState(ServerSideStates.STARTING);			
			log.info(String.format("Responding with OkResponseInitHandover to %s", getSender()));
			getSender().tell(IOStationCapability.ServerMessageTypes.OkResponseInitHandover, self);
			context().system()
	    	.scheduler()
	    	.scheduleOnce(Duration.ofMillis(200), 
	    			 new Runnable() {
	            @Override
	            public void run() {
	            	publishNewState(ServerSideStates.PREPARING);
	            	if (isLoaded) {	            		
	            		publishNewState(ServerSideStates.READY_LOADED);
	            	} else {	            		
	            		publishNewState(ServerSideStates.READY_EMPTY);
	            	}
	            }
	          }, context().system().dispatcher());
		} else if (currentState.equals(ServerSideStates.READY_EMPTY) || currentState.equals(ServerSideStates.READY_LOADED)){
			getSender().tell(IOStationCapability.ServerMessageTypes.OkResponseInitHandover, self); // resending
		} else {
			log.warning(String.format("Responding with NotOkResponseInitHandover to %s in state %s", getSender(), currentState));
			getSender().tell(IOStationCapability.ServerMessageTypes.NotOkResponseInitHandover, self);
		}
		
	} 		
	
	private void  startHandover() {
		if ((currentState.equals(ServerSideStates.READY_EMPTY) || currentState.equals(ServerSideStates.READY_LOADED))) {
			publishNewState(ServerSideStates.EXECUTE);
			log.info(String.format("Responding with OkResponseStartHandover to %s", getSender()));
			getSender().tell(IOStationCapability.ServerMessageTypes.OkResponseStartHandover, self);
		} else if ( currentState.equals(ServerSideStates.EXECUTE) ){ //resending
			getSender().tell(IOStationCapability.ServerMessageTypes.OkResponseStartHandover, self);
		} else {
			log.warning(String.format("Responding with NotOkResponseStartHandover to %s in state %s", getSender(), currentState));
			getSender().tell(IOStationCapability.ServerMessageTypes.NotOkResponseStartHandover, self);
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
		publishNewState(ServerSideStates.COMPLETING);
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000), 
    			 new Runnable() {
            @Override
            public void run() {
            	// we toggle load flag as when we were empty now we are loaded and vice versa
            	isLoaded = !isLoaded;
            	publishNewState(ServerSideStates.COMPLETE); 
//            	stop(); // we automatically stop --> no longer stop, just remain in complete
            }
          }, context().system().dispatcher());
	}			
	
	private void stop() {
		publishNewState(ServerSideStates.STOPPING);
		//clientSide = null;
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000), 
    			 new Runnable() {
            @Override
            public void run() {
            	publishNewState(ServerSideStates.STOPPED);
            }
          }, context().system().dispatcher());
	}
}
