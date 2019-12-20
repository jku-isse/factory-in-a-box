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
import fiab.mes.transport.handshake.HandshakeProtocol.ServerSide;
import fiab.opcua.hardwaremock.InputStationMock;


public class MockServerHandshakeActor extends AbstractActor{

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);	
	protected boolean isLoaded = false; //assume at bootup that no pallet is loaded
	private ActorRef machineWrapper;
	protected ServerSide currentState = ServerSide.Stopped;
	protected ActorRef clientSide;
	protected ActorRef self;
	protected Set<ActorRef> subscribers = new HashSet<ActorRef>();
	protected boolean doAutoComplete = false;
	private InputStationMock ism = null;
	
	static public Props props(ActorRef machineWrapper, boolean doAutoComplete) {	    
		return Props.create(MockServerHandshakeActor.class, () -> new MockServerHandshakeActor(machineWrapper, doAutoComplete));
	}
	
	public MockServerHandshakeActor(ActorRef machineWrapper, boolean doAutoComplete) {
		this.machineWrapper = machineWrapper;
		this.doAutoComplete = doAutoComplete;
		self = getSelf();
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(MessageTypes.class, msg -> {
					log.info(String.format("Received %s from %s", msg, getSender()));
					switch(msg) {					
					case Complete:
						if (currentState.equals(ServerSide.Execute)) {
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
						if (currentState.equals(ServerSide.Stopped) || currentState.equals(ServerSide.Completed)) {
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
				.match(InputStationMock.class, ism -> {
					this.ism = ism;
				})
				.matchAny(msg -> { 
					log.warning("Unexpected Message received: "+msg.toString()); })
		        .build();
	}

	protected void publishNewState(ServerSide newState) {
		currentState = newState;
		if (machineWrapper != null) {
			machineWrapper.tell(newState, self);
		}
		if(ism != null) {
			ism.setStatusValue(newState.name());
		}
		ImmutableSet.copyOf(subscribers).stream().forEach(sub -> sub.tell(newState, self));
	}
	
	protected void updateLoadState(boolean isLoaded) {
		if (this.isLoaded != isLoaded) {
			this.isLoaded = isLoaded;
			if (currentState != ServerSide.Stopped)
				stop();
		}
	}
	
	protected void reset() {
		publishNewState(ServerSide.Resetting);
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
			clientSide.tell(MessageTypes.OkResponseInitHandover, self);
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
			getSender().tell(MessageTypes.NotOkResponseInitHandover, self);
		}
		
	} 		
	
	private void  startHandover() {
		if ((currentState.equals(ServerSide.ReadyEmpty) || currentState.equals(ServerSide.ReadyLoaded)) && getSender().equals(clientSide)) {
			publishNewState(ServerSide.Execute);
			log.info(String.format("Responding with OkResponseStartHandover to %s", getSender()));
			clientSide.tell(MessageTypes.OkResponseStartHandover, self);
		} else {
			log.warning(String.format("Responding with NotOkResponseStartHandover to %s in state %s", getSender(), currentState));
			clientSide.tell(MessageTypes.NotOkResponseStartHandover, self);
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
		publishNewState(ServerSide.Completing);
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000), 
    			 new Runnable() {
            @Override
            public void run() {
            	// we toggle load flag as when we were empty now we are loaded and vice versa
            	isLoaded = !isLoaded;
            	publishNewState(ServerSide.Completed); 
//            	stop(); // we automatically stop --> no longer stop, just remain in complete
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
	
	public static enum StateOverrideRequests {
		SetLoaded, SetEmpty
	}
}
