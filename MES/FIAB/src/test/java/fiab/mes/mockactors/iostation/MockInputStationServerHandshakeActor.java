package fiab.mes.mockactors.iostation;

import java.time.Duration;
import java.util.Set;

import com.google.common.collect.Sets;

import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.mes.mockactors.MockServerHandshakeActor;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerSide;

public class MockInputStationServerHandshakeActor extends MockServerHandshakeActor{

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);	
	
	static public Props props() {	    
		return Props.create(MockInputStationServerHandshakeActor.class, () -> new MockInputStationServerHandshakeActor());
	}
	
	public MockInputStationServerHandshakeActor() {
		super(null, true);
	}

	// we accept the usual request but we implement an auto reset, except for an initial reset to become active
	// and awaiting for being loaded in the reset method, i.e., we wont ever reach idleEmpty
	
	@Override
	protected void reset() {
		publishNewState(ServerSide.Resetting);
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000), 
    			 new Runnable() {
            @Override
            public void run() {
            	if (isLoaded && !currentState.equals(ServerSide.IdleLoaded)) {
            		publishNewState(ServerSide.IdleLoaded);
            	} else {
            		// stay in resetting
            	}
            }
          }, context().system().dispatcher());
	}
	
	private Set<ServerSide> loadChangeableStates = Sets.newHashSet(ServerSide.Completed, ServerSide.Completing, ServerSide.Stopped, ServerSide.Stopping);
	
	@Override
	protected void updateLoadState(boolean isLoaded) {
		log.info("Updating Loading State");
		if (this.isLoaded != isLoaded) {
			this.isLoaded = isLoaded;
			if (currentState.equals(ServerSide.Resetting)) {
				reset(); // we reset again to reach IdleLoaded
			} else if (!loadChangeableStates.contains(currentState)) {
				stopAndAutoReset();
			}
		}
	}
	
	private void stopAndAutoReset() {
		
		publishNewState(ServerSide.Stopping);
		clientSide = null;
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000), 
    			 new Runnable() {
            @Override
            public void run() {
            	publishNewState(ServerSide.Stopped);
            	reset();
            }
          }, context().system().dispatcher());
	}
	
}
