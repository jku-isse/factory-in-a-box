package fiab.mes.mockactors;

import java.time.Duration;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.mes.transport.handshake.HandshakeProtocol.ClientSide;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerSide;


public class MockClientHandshakeActor extends AbstractActor{

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);	
	private ActorRef machineWrapper;
	private ActorRef serverSide;
	private ClientSide currentState = ClientSide.STOPPED;	
	private ServerSide remoteState = null;
	private ActorRef self;
	
	static public Props props(ActorRef machineWrapper, ActorRef serverSide) {	    
		return Props.create(MockClientHandshakeActor.class, () -> new MockClientHandshakeActor(machineWrapper, serverSide));
	}
	
	public MockClientHandshakeActor(ActorRef machineWrapper, ActorRef serverSide) {
		this.machineWrapper = machineWrapper;
		this.serverSide = serverSide;
		this.self = getSelf();
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(MessageTypes.class, msg -> { // commands from parent FU
					switch(msg) {					
					case Reset:
						reset(); // prepare for next round
						break;
					case Start:
						start(); // engage in handshake: subscribe to state updates
						break;
					case Complete:
						complete(); // handshake can be wrapped up
						break;
					case Stop:
						stop(); // error or external stop, otherwise autostopping upon completion
						break;
					default:
						break;
					}
				})
				.match(MockServerHandshakeActor.MessageTypes.class , resp -> { // responses to requests
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
						case COMPLETED: //fallthrough, if serverside is done, we can do the same 
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
			serverSide.tell(MockServerHandshakeActor.MessageTypes.SubscribeToStateUpdates, getSelf()); //subscribe for updates
			publishNewState(ClientSide.INITIATING);
		} else {
			log.warning("was requested invalid command 'Start' in state: "+currentState); 
		}
	} 		
	
	private void requestInitiateHandover() {
		getSender().tell(MockServerHandshakeActor.MessageTypes.RequestInitiateHandover, self);
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
			serverSide.tell(MockServerHandshakeActor.MessageTypes.RequestStartHandover, self);
		} else {
			log.warning("was requested invalid command 'StartHandover' in state: "+currentState); 
		}				
	}
	
	private void receiveStartOkResponse() {
		publishNewState(ClientSide.EXECUTE);
	}
	
	private void complete() {
		publishNewState(ClientSide.COMPLETING);
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000), 
    			 new Runnable() {
            @Override
            public void run() {
            	publishNewState(ClientSide.COMPLETED); 
            	//stop(); // we automatically stop --> we no longer stop but remain in completed, reactiving via reset()
            }
          }, context().system().dispatcher());
	}			
	
	private void stop() {
		publishNewState(ClientSide.STOPPING);
		if (serverSide != null) {
			serverSide.tell(MockServerHandshakeActor.MessageTypes.UnsubscribeToStateUpdates, self);
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
	
	public static enum MessageTypes {
		Reset, Stop, Start, Complete
	}
}
