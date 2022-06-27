package fiab.machine.iostation;

import java.time.Duration;

import akka.actor.ActorRef;
import akka.actor.Props;
import fiab.core.capabilities.StatePublisher;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.handshake.actor.ServerSideHandshakeActor;

public class IOStationServerHandshakeActor extends ServerSideHandshakeActor {

	private boolean isOutputStation;
	
	static public Props propsForOutputstation(ActorRef machineWrapper, boolean doAutoComplete, StatePublisher publishEP) {	    
		return Props.create(IOStationServerHandshakeActor.class, () -> new IOStationServerHandshakeActor(machineWrapper, doAutoComplete, publishEP, true));
	}
	
	static public Props propsForInputstation(ActorRef machineWrapper, boolean doAutoComplete, StatePublisher publishEP) {	    
		return Props.create(IOStationServerHandshakeActor.class, () -> new IOStationServerHandshakeActor(machineWrapper, doAutoComplete, publishEP, false));
	}
	
	public IOStationServerHandshakeActor(ActorRef machineWrapper, boolean doAutoComplete, StatePublisher publishEP, boolean isOutputStation) {
		super(machineWrapper, doAutoComplete, publishEP);
		this.isOutputStation = isOutputStation;
	}
	
//	@Override
//	protected void publishNewState(ServerSideStates newState) {
//		currentState = newState;
//		ImmutableSet.copyOf(subscribers).stream().forEach(sub -> sub.tell(newState, getSelf()));
//	}

	// we accept the usual request but we implement an auto reset, except for an initial reset to become active
	// and awaiting for being empty in the reset method, i.e., we wont ever reach idleFull
	
	@Override
	protected void reset() {
		publishNewState(ServerSideStates.RESETTING);
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000), 
    			 new Runnable() {
            @Override
            public void run() { // if we are still in resetting
            	if (currentState != ServerSideStates.RESETTING) return;
            	tryProgressBeyondResetting();
            }
          }, context().system().dispatcher());
	}
	
	private void tryProgressBeyondResetting() {
		if (isLoaded && !isOutputStation) {
			publishNewState(ServerSideStates.IDLE_LOADED); //we only progress if we are inputstation and loaded
		} else if (!isLoaded && isOutputStation) {
			publishNewState(ServerSideStates.IDLE_EMPTY); // or if we are outputstation and empty
		}
	}
	
	@Override
	protected boolean updateLoadState(boolean isLoaded) {
		if (this.isLoaded == isLoaded) return false; 
		boolean success = super.updateLoadState(isLoaded);
		if (!success && currentState.equals(ServerSideStates.RESETTING)) {
			this.isLoaded = isLoaded;
			tryProgressBeyondResetting();
			return true;
		}
		return false;
//		if (this.isLoaded != isLoaded) {
//			this.isLoaded = isLoaded;
//			if () {
//				reset(); // we reset again to reach IdleLoaded
//			} else if (currentState != ServerSideStates.STOPPED) {
//				stopAndAutoReset();
//			}
//		}
	}
	
//	private void stopAndAutoReset() {
//		publishNewState(ServerSideStates.STOPPING);
//		//clientSide = null;
//		context().system()
//    	.scheduler()
//    	.scheduleOnce(Duration.ofMillis(1000), 
//    			 new Runnable() {
//            @Override
//            public void run() {
//            	publishNewState(ServerSideStates.STOPPED);
//            	reset();
//            }
//          }, context().system().dispatcher());
//	}
	
}
