package fiab.mes.mockactors.iostation;

import java.time.Duration;

import com.google.common.collect.ImmutableSet;

import akka.actor.ActorRef;
import akka.actor.Props;
import fiab.mes.mockactors.MockServerHandshakeActor;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerSide;

public class MockOutputStationServerHandshakeActor extends MockServerHandshakeActor{

	static public Props props() {	    
		return Props.create(MockOutputStationServerHandshakeActor.class, () -> new MockOutputStationServerHandshakeActor());
	}
	
	public MockOutputStationServerHandshakeActor() {
		super(null, true, null);
	}
	
	@Override
	protected void publishNewState(ServerSide newState) {
		currentState = newState;
		ImmutableSet.copyOf(subscribers).stream().forEach(sub -> sub.tell(newState, getSelf()));
	}

	// we accept the usual request but we implement an auto reset, except for an initial reset to become active
	// and awaiting for being empty in the reset method, i.e., we wont ever reach idleFull
	
	@Override
	protected void reset() {
		publishNewState(ServerSide.RESETTING);
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000), 
    			 new Runnable() {
            @Override
            public void run() {
            	if (!isLoaded) {
            		publishNewState(ServerSide.IDLE_EMPTY);
            	} else {
            		// stay in resetting
            	}
            }
          }, context().system().dispatcher());
	}
	
	@Override
	protected void updateLoadState(boolean isLoaded) {
		if (this.isLoaded != isLoaded) {
			this.isLoaded = isLoaded;
			if (currentState.equals(ServerSide.RESETTING)) {
				reset(); // we reset again to reach IdleLoaded
			} else if (currentState != ServerSide.STOPPED) {
				stopAndAutoReset();
			}
		}
	}
	
	private void stopAndAutoReset() {
		publishNewState(ServerSide.STOPPING);
		//clientSide = null;
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000), 
    			 new Runnable() {
            @Override
            public void run() {
            	publishNewState(ServerSide.STOPPED);
            	reset();
            }
          }, context().system().dispatcher());
	}
	
}
