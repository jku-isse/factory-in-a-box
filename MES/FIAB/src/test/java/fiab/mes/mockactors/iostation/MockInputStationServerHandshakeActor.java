package fiab.mes.mockactors.iostation;

import java.time.Duration;
import java.util.Set;

import com.google.common.collect.Sets;

import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.core.capabilities.handshake.HandshakeCapability.ServerSideStates;
import fiab.handshake.actor.ServerSideHandshakeActor;

public class MockInputStationServerHandshakeActor extends ServerSideHandshakeActor{

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);	
	
	static public Props props() {	    
		return Props.create(MockInputStationServerHandshakeActor.class, () -> new MockInputStationServerHandshakeActor());
	}
	
	public MockInputStationServerHandshakeActor() {
		super(null, true, null);
	}

	// we accept the usual request but we implement an auto reset, except for an initial reset to become active
	// and awaiting for being loaded in the reset method, i.e., we wont ever reach idleEmpty
	
	@Override
	protected void reset() {
		publishNewState(ServerSideStates.RESETTING);
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000), 
    			 new Runnable() {
            @Override
            public void run() {
            	if (isLoaded && !currentState.equals(ServerSideStates.IDLE_LOADED)) {
            		publishNewState(ServerSideStates.IDLE_LOADED);
            	} else {
            		// stay in resetting
            	}
            }
          }, context().system().dispatcher());
	}
	
	private Set<ServerSideStates> loadChangeableStates = Sets.newHashSet(ServerSideStates.COMPLETE, ServerSideStates.COMPLETING, ServerSideStates.STOPPED, ServerSideStates.STOPPING);
	
	@Override
	protected void updateLoadState(boolean isLoaded) {
		log.info("Updating Loading State");
		if (this.isLoaded != isLoaded) {
			this.isLoaded = isLoaded;
			if (currentState.equals(ServerSideStates.RESETTING)) {
				reset(); // we reset again to reach IdleLoaded
			} else if (!loadChangeableStates.contains(currentState)) {
				stopAndAutoReset();
			}
		}
	}
	
	private void stopAndAutoReset() {
		
		publishNewState(ServerSideStates.STOPPING);
		//clientSide = null;
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000), 
    			 new Runnable() {
            @Override
            public void run() {
            	publishNewState(ServerSideStates.STOPPED);
            	reset();
            }
          }, context().system().dispatcher());
	}
	
}
