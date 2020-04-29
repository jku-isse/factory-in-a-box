package fiab.opcua.hardwaremock.iostation;

import java.time.Duration;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.core.capabilities.StatePublisher;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.core.capabilities.handshake.HandshakeCapability.ServerSideStates;
import fiab.machine.iostation.IOStationServerHandshakeActor;
import fiab.core.capabilities.handshake.IOStationCapability;

public class OPCUAVirtualIOStationWrapper extends AbstractActor {

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	protected boolean doPublishState = true;
	protected ServerSideStates handshakeStatus = ServerSideStates.STOPPED;
	protected ActorRef serverSide;
	protected ActorRef self;
	protected boolean isInputStation = true;
	protected boolean doAutoReload;
	protected StatePublisher spub;
	
	static public Props props(boolean isInputStation, boolean doAutoReload) {	    
		return Props.create(OPCUAVirtualIOStationWrapper.class, () -> new OPCUAVirtualIOStationWrapper(isInputStation, doAutoReload));
	}
	
	public OPCUAVirtualIOStationWrapper(boolean isInputStation,boolean doAutoReload) {
		this.isInputStation = isInputStation;
		this.doAutoReload = doAutoReload;
		self = getSelf();
		if (isInputStation)
			serverSide = getContext().actorOf(IOStationServerHandshakeActor.propsForInputstation(), "InputStationServerSideHandshakeMock"); 
		else 
			serverSide = getContext().actorOf(IOStationServerHandshakeActor.propsForOutputstation(), "OutputStationServerSideHandshakeMock"); 
		reloadPallet();
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(IOStationCapability.ServerMessageTypes.class, msg -> {
					switch(msg) {
					case SubscribeToStateUpdates: 
						doPublishState = true;
						//setAndPublishState(handshakeStatus); //we publish the current state
						serverSide.tell(msg, self); //we subscribe to iostationserverhandshake
						break;
					default:
						serverSide.forward(msg, getContext()); //but everything else is forward to iostationserverhandshake
					}
				})
				.match(ServerSideStates.class, msg -> { // state event updates from handshake, pass upward
					if (getSender().equals(serverSide)) {
						handshakeStatus = msg;
						setAndPublishState(msg);
						if (msg.equals(ServerSideStates.COMPLETE)) { //we auto reload here
							context().system()
					    	.scheduler()
					    	.scheduleOnce(Duration.ofMillis(1000), 
					    			 new Runnable() {
					            @Override
					            public void run() {
					            	if (doAutoReload) {
					            		reloadPallet();
					            	}
					            }
					          }, context().system().dispatcher());
						}
					}
				})
				.match(StatePublisher.class, pub -> this.spub = pub)
				.matchAny(msg -> { 
						serverSide.tell(msg, self); }) //we forward everything else to iostationserverhandshake
				.build();
	}

	private void setAndPublishState(ServerSideStates newState) {
		if (doPublishState && spub != null) {
			//interEventBus.publish(new IOStationStatusUpdateEvent(self.path().toString(), "", newState));
			spub.setStatusValue(newState.toString());
		}
	}
	
	private void reloadPallet() {
		//tell handshake that the pallet is loaded if inputstation, otherwise setempty
			log.info(self.path().name()+": Auto Reloading Pallet");
			if (isInputStation) {
				serverSide.tell(HandshakeCapability.StateOverrideRequests.SetLoaded, self); 
			} else {
				serverSide.tell(HandshakeCapability.StateOverrideRequests.SetEmpty, self); 
			}
	}
	
}
