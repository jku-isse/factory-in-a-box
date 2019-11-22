package fiab.mes.mockactors;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.mes.handshake.HandshakeProtocol.ServerSide;


public class MockServerHandshakeActor extends AbstractActor{

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);	
	private boolean isLoaded = false; //assume at bootup that no pallet is loaded
	private ActorRef machineWrapper;
	private ServerSide currentState = ServerSide.Stopped;
	private ActorRef clientSide;
	private Set<ActorRef> subscribers = new HashSet<ActorRef>();
	
	static public Props props(ActorRef machineWrapper) {	    
		return Props.create(MockServerHandshakeActor.class, () -> new MockServerHandshakeActor(machineWrapper));
	}
	
	public MockServerHandshakeActor(ActorRef machineWrapper) {
		this.machineWrapper = machineWrapper;
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(MessageTypes.class, msg -> {
					switch(msg) {					
					case Complete:
						complete();
						break;
					case RequestInitiateHandover:
						log.info(String.format("Received %s from %s", msg, getSender()));
						initHandover();
						break;
					case RequestStartHandover:
						log.info(String.format("Received %s from %s", msg, getSender()));
						startHandover();
						break;
					case Reset:
						reset();
						break;
					case Stop:
						stop();
						break;
					case SubscribeToStateUpdates:
						subscribers.add(getSender());
						getSender().tell(currentState, getSelf()); // update subscriber with current state
					case UnsubscribeToStateUpdates:
						subscribers.remove(getSender());
					default:
						break;
					}
				})
				.matchAny(msg -> { 
					log.warning("Unexpected Message received: "+msg.toString()); })
		        .build();
	}

	private void publishNewState(ServerSide newState) {
		currentState = newState;
		machineWrapper.tell(newState, getSelf());
//		if (clientSide != null) { // update client about status (no polling, or pub/sub here in mockactor)
//			clientSide.tell(newState, getSelf());
//		}
		subscribers.stream().forEach(sub -> sub.tell(newState, getSelf()));
	}
	
	private void reset() {
		publishNewState(ServerSide.Resetting);
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
            	if (isLoaded) {
            		publishNewState(ServerSide.IdleLoaded);
            	} else {
            		publishNewState(ServerSide.IdleEmpty);
            	}
            }
          }, context().system().dispatcher());
	}
	
	
	
	private void initHandover() {
		if (currentState.equals(ServerSide.IdleEmpty) || currentState.equals(ServerSide.IdleLoaded)) {
			publishNewState(ServerSide.Starting);
			clientSide = getSender();
			log.info(String.format("Responding with OkResponseInitHandover to %s", getSender()));
			clientSide.tell(MessageTypes.OkResponseInitHandover, getSelf());
			context().system()
	    	.scheduler()
	    	.scheduleOnce(Duration.ofMillis(200), 
	    			 new Runnable() {
	            @Override
	            public void run() {
	            	publishNewState(ServerSide.Preparing);
	            	if (isLoaded) {	            		
	            		publishNewState(ServerSide.ReadyLoaded);
	            	} else {	            		
	            		publishNewState(ServerSide.ReadyEmpty);
	            	}
	            }
	          }, context().system().dispatcher());
		} else {
			log.warning(String.format("Responding with NotOkResponseInitHandover to %s in state %s", getSender(), currentState));
			getSender().tell(MessageTypes.NotOkResponseInitHandover, getSelf());
		}
		
	} 		
	
	private void  startHandover() {
		if ((currentState.equals(ServerSide.ReadyEmpty) || currentState.equals(ServerSide.ReadyLoaded)) && getSender().equals(clientSide)) {
			publishNewState(ServerSide.Execute);
			log.info(String.format("Responding with OkResponseStartHandover to %s", getSender()));
			clientSide.tell(MessageTypes.OkResponseStartHandover, getSelf());
		} else {
			log.warning(String.format("Responding with NotOkResponseStartHandover to %s in state %s", getSender(), currentState));
			clientSide.tell(MessageTypes.NotOkResponseStartHandover, getSelf());
		}				
	}
	
	private void complete() {
		publishNewState(ServerSide.Completing);
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000), 
    			 new Runnable() {
            @Override
            public void run() {
            	// we toggle load flag as when we were empty now we are loaded and vice versa
            	isLoaded = !isLoaded;
            	publishNewState(ServerSide.Complete); 
            	stop(); // we automatically stop
            }
          }, context().system().dispatcher());
	}			
	
	private void stop() {
		publishNewState(ServerSide.Stopping);
		clientSide = null;
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000), 
    			 new Runnable() {
            @Override
            public void run() {
            	publishNewState(ServerSide.Stopped);
            }
          }, context().system().dispatcher());
	}	
	
	public static enum MessageTypes {
		Reset, Stop, RequestInitiateHandover, OkResponseInitHandover, NotOkResponseInitHandover, 
		RequestStartHandover, OkResponseStartHandover, NotOkResponseStartHandover, Complete, 
		SubscribeToStateUpdates, UnsubscribeToStateUpdates 
	}
}
