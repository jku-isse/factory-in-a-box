package fiab.mes.mockactors;

import java.time.Duration;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.core.capabilities.handshake.IOStationCapability;
import fiab.core.capabilities.handshake.HandshakeCapability.ClientSide;
import fiab.core.capabilities.handshake.HandshakeCapability.ServerSide;
import fiab.opcua.hardwaremock.StatePublisher;


public class MockClientHandshakeActor extends AbstractActor{

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);	
	private ActorRef machineWrapper;
	private ActorRef serverSide;
	private ClientSide currentState = ClientSide.STOPPED;	
	private ServerSide remoteState = null;
	private ActorRef self;
	
	private StatePublisher publishEP;
	
	static public Props props(ActorRef machineWrapper, ActorRef serverSide) {	    
		return Props.create(MockClientHandshakeActor.class, () -> new MockClientHandshakeActor(machineWrapper, serverSide));
	}
	
	static public Props props(ActorRef machineWrapper, ActorRef serverSide, StatePublisher publishEP) {	    
		return Props.create(MockClientHandshakeActor.class, () -> new MockClientHandshakeActor(machineWrapper, serverSide, publishEP));
	}
	
	public MockClientHandshakeActor(ActorRef machineWrapper, ActorRef serverSide) {
		this.machineWrapper = machineWrapper;
		this.serverSide = serverSide;
		this.self = getSelf();
	}
	
	public MockClientHandshakeActor(ActorRef machineWrapper, ActorRef serverSide, StatePublisher publishEP) {
		this.machineWrapper = machineWrapper;
		this.serverSide = serverSide;
		this.publishEP = publishEP;
		this.self = getSelf();
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(IOStationCapability.ClientMessageTypes.class, msg -> { // commands from parent FU
					switch(msg) {					
					case Reset:
						reset(); // prepare for next round
						break;
					case Start:
						start(); // engage in handshake: subscribe to state updates
						break;
					case Complete:
						if (currentState.equals(ClientSide.EXECUTE)) // only if we are in state executing, otherwise complete makes no sense
							complete(); // handshake can be wrapped up
						break;
					case Stop:
						stop(); // error or external stop, otherwise autostopping upon completion
						break;
					default:
						break;
					}
				})
				.match(IOStationCapability.ServerMessageTypes.class , resp -> { // responses to requests
					switch(resp) {				
					case NotOkResponseInitHandover:
						stop();
						break;
					case NotOkResponseStartHandover:
						stop();
						break;
					case OkResponseInitHandover:
						receiveInitiateOkResponse();
						break;
					case OkResponseStartHandover:
						receiveStartOkResponse();
						break;					
					default:
						log.warning("Unexpected ServerSide MessageType received: "+resp.toString()); 
						break;					
					}
				})				
				.match(ServerSide.class, msg -> { // state event updates
					log.info(String.format("Received %s from %s in local state %s", msg, getSender(), currentState));
					if (getSender().equals(serverSide)) {
						remoteState = msg;
						switch(msg) {
						case IDLE_EMPTY: //fallthrough
						case IDLE_LOADED:
							if (currentState.equals(ClientSide.STARTING) || currentState.equals(ClientSide.INITIATING))
							requestInitiateHandover();
							break;
						case COMPLETE: //fallthrough, if serverside is done, we can do the same 
						case COMPLETING:
							// onlfy if in executing
							if (currentState.equals(ClientSide.EXECUTE))
								complete();
							break;
						case EXECUTE: //if server side is executing, we can do the same if we are in ready
							if (currentState.equals(ClientSide.READY))
								receiveStartOkResponse();
							break;
						case READY_EMPTY: // fallthrough
						case READY_LOADED:
							if (currentState.equals(ClientSide.READY)) {
								requestStartHandover();
							}
							break;
						case STOPPED: // fallthrough
						case STOPPING: // if we are in any state that would not expect a stop, then we also need to stop/abort
							if (//currentState.equals(ClientSide.Initiating) ||
								//	currentState.equals(ClientSide.Initiated) || //the server might not be ready yet, thus we need to wait, not stop
									currentState.equals(ClientSide.READY) ||
									currentState.equals(ClientSide.EXECUTE)	)
								stop();
							break;
						default:
							break;
						}
					} else {
						log.warning(String.format("Received %s to unexpected sender %s", msg, getSender()));
					}
				})
				.matchAny(msg -> { 
					log.warning("Unexpected Message received: "+msg.toString()); })
		        .build();
	}

	private void publishNewState(ClientSide newState) {
		currentState = newState;
		machineWrapper.tell(newState, getSelf());	
		if (publishEP != null)
			publishEP.setStatusValue(newState.toString());
	}
	
	private void reset() {
		publishNewState(ClientSide.RESETTING);
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
            		publishNewState(ClientSide.IDLE);           		
            }
          }, context().system().dispatcher());
	}
	
	private void start() {
		if (currentState.equals(ClientSide.IDLE)) {
			publishNewState(ClientSide.STARTING);
			serverSide.tell(IOStationCapability.ServerMessageTypes.SubscribeToStateUpdates, getSelf()); //subscribe for updates
			publishNewState(ClientSide.INITIATING);
		} else {
			log.warning("was requested invalid command 'Start' in state: "+currentState); 
		}
	} 		
	
	private void requestInitiateHandover() {
		getSender().tell(IOStationCapability.ServerMessageTypes.RequestInitiateHandover, self);
		retryInit();
	}
	
	private void retryInit() {
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(5000), 
    			 new Runnable() {
            @Override
            public void run() {
            	// if still waiting:
            	if (currentState.equals(ClientSide.INITIATING)) {
            		log.info("Retrying to send InitiateHandover");
            		requestInitiateHandover();
            	}            		            	
            }
          }, context().system().dispatcher());
	}
	
	private void receiveInitiateOkResponse() {		
		publishNewState(ClientSide.INITIATED);
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(2000), 
    			 new Runnable() {
            @Override
            public void run() {
            	publishNewState(ClientSide.READY); 
            	if (remoteState.equals(ServerSide.READY_EMPTY) || remoteState.equals(ServerSide.READY_LOADED)){
            		// only of remote point is ready, and we are also still ready (as this is triggered some time in the future)
            		if (currentState.equals(ClientSide.READY))
            			requestStartHandover();
            	} else {
            		log.info(String.format("Server %s in last known state %s not yet ready for RequestStartHandover", serverSide, remoteState));
            	}
            }
          }, context().system().dispatcher());
		
	}
	
	private void  requestStartHandover() {
		if (currentState.equals(ClientSide.READY) ) {	
			log.info(String.format("Requesting StartHandover from remote %s", serverSide));
			serverSide.tell(IOStationCapability.ServerMessageTypes.RequestStartHandover, self);
			retryStartHandover();
		} else {
			log.warning("was requested invalid command 'StartHandover' in state: "+currentState); 
		}				
	}
	
	private void retryStartHandover() {		
			context().system()
	    	.scheduler()
	    	.scheduleOnce(Duration.ofMillis(3000), 
	    			 new Runnable() {
	            @Override
	            public void run() {
	            	// if still Ready:
	            	if (currentState.equals(ClientSide.READY)) {	            			            		
	            		log.info("Retrying to send StartHandover");
	            		requestStartHandover();
	            	}            		            	
	            }
	          }, context().system().dispatcher());		
	}
	
	
	
	private void receiveStartOkResponse() {
		publishNewState(ClientSide.EXECUTE);
	}
	
	private void complete() {
		publishNewState(ClientSide.COMPLETING);
		if (serverSide != null) {
			serverSide.tell(IOStationCapability.ServerMessageTypes.UnsubscribeToStateUpdates, self);
		}
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000), 
    			 new Runnable() {
            @Override
            public void run() {
            	publishNewState(ClientSide.COMPLETED); 
            }
          }, context().system().dispatcher());
	}			
	
	private void stop() {
		publishNewState(ClientSide.STOPPING);
		if (serverSide != null) {
			serverSide.tell(IOStationCapability.ServerMessageTypes.UnsubscribeToStateUpdates, self);
		}
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000), 
    			 new Runnable() {
            @Override
            public void run() {
            	publishNewState(ClientSide.STOPPED);
            }
          }, context().system().dispatcher());
	}
}
