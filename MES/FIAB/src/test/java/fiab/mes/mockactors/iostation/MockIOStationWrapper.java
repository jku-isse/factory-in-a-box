package fiab.mes.mockactors.iostation;

import java.time.Duration;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.core.capabilities.handshake.IOStationCapability;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.core.capabilities.handshake.HandshakeCapability.ServerSideStates;
import fiab.core.capabilities.handshake.HandshakeCapability.StateOverrideRequests;
import fiab.machine.iostation.IOStationServerHandshakeActor;
import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;

public class MockIOStationWrapper extends AbstractActor {

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	protected InterMachineEventBus interEventBus;
	protected boolean doPublishState = false;
	protected ServerSideStates handshakeStatus = ServerSideStates.STOPPED;
	protected ActorRef serverSide;
	protected ActorRef self;
	protected boolean isInputStation = true;
	protected boolean doAutoReload;
	
	static public Props props(InterMachineEventBus internalMachineEventBus, boolean isInputStation, boolean doAutoReload) {	    
		return Props.create(MockIOStationWrapper.class, () -> new MockIOStationWrapper(internalMachineEventBus, isInputStation, doAutoReload));
	}
	
	public MockIOStationWrapper(InterMachineEventBus machineEventBus, boolean isInputStation,boolean doAutoReload) {
		this.interEventBus = machineEventBus;
		this.isInputStation = isInputStation;
		this.doAutoReload = doAutoReload;
		self = getSelf();
		if (isInputStation)
			serverSide = getContext().actorOf(MockInputStationServerHandshakeActor.props(), "InputStationServerSideHandshakeMock"); 
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
						//fallthrough
					default:
						serverSide.tell(msg, self); //forward to iostationserverhandshake
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
				.matchAny(msg -> { 
						serverSide.tell(msg, self); }) //we forward everything else to iostationserverhandshake
				.build();
	}

	private void setAndPublishState(ServerSideStates newState) {
		if (doPublishState) {
			interEventBus.publish(new IOStationStatusUpdateEvent(self.path().toString(), "", newState));
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
