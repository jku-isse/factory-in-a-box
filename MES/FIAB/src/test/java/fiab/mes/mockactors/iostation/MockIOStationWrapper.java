package fiab.mes.mockactors.iostation;

import java.time.Duration;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.mockactors.MockServerHandshakeActor.StateOverrideRequests;
import fiab.mes.transport.handshake.HandshakeProtocol;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerMessageTypes;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerSide;

public class MockIOStationWrapper extends AbstractActor {

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	protected InterMachineEventBus interEventBus;
	protected boolean doPublishState = false;
	protected ServerSide handshakeStatus = ServerSide.STOPPED;
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
			serverSide = getContext().actorOf(MockOutputStationServerHandshakeActor.props(), "OutputStationServerSideHandshakeMock"); 
		reloadPallet();
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(HandshakeProtocol.ServerMessageTypes.class, msg -> {
					switch(msg) {
					case SubscribeToStateUpdates: 
						doPublishState = true;
						//setAndPublishState(handshakeStatus); //we publish the current state
						//fallthrough
					default:
						serverSide.tell(msg, self); //forward to iostationserverhandshake
					}
				})
				.match(ServerSide.class, msg -> { // state event updates from handshake, pass upward
					if (getSender().equals(serverSide)) {
						handshakeStatus = msg;
						setAndPublishState(msg);
						if (msg.equals(ServerSide.COMPLETE)) { //we auto reload here
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

	private void setAndPublishState(ServerSide newState) {
		if (doPublishState) {
			interEventBus.publish(new IOStationStatusUpdateEvent(self.path().toString(), "", newState));
		}
	}
	
	private void reloadPallet() {
		//tell handshake that the pallet is loaded if inputstation, otherwise setempty
			log.info(self.path().name()+": Auto Reloading Pallet");
			if (isInputStation) {
				serverSide.tell(StateOverrideRequests.SetLoaded, self); 
			} else {
				serverSide.tell(StateOverrideRequests.SetEmpty, self); 
			}
	}
	
}
