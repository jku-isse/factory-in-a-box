package fiab.mes.mockactors.iostation;

import java.time.Duration;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.mockactors.MockServerHandshakeActor.MessageTypes;
import fiab.mes.mockactors.MockServerHandshakeActor.StateOverrideRequests;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerSide;

public class MockIOStationWrapper extends AbstractActor {

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	protected InterMachineEventBus interEventBus;
	protected boolean doPublishState = false;
	protected ServerSide handshakeStatus = ServerSide.Stopped;
	protected ActorRef serverSide;
	protected ActorRef self;
	protected boolean isInputStation = true;
	
	static public Props props(InterMachineEventBus internalMachineEventBus, boolean isInputStation) {	    
		return Props.create(MockIOStationWrapper.class, () -> new MockIOStationWrapper(internalMachineEventBus, isInputStation));
	}
	
	public MockIOStationWrapper(InterMachineEventBus machineEventBus, boolean isInputStation) {
		this.interEventBus = machineEventBus;
		this.isInputStation = isInputStation;
		// setup serverhandshake actor with autocomplete
		boolean doAutoComplete = true;
		self = getSelf();
		serverSide = getContext().actorOf(MockInputStationServerHandshakeActor.props(getSelf(), doAutoComplete), "IOStationServerSideHandshakeMock"); 
		reloadPallet();
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(MessageTypes.class, msg -> {
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
						if (msg.equals(ServerSide.Completed)) { //we auto reload here
							context().system()
					    	.scheduler()
					    	.scheduleOnce(Duration.ofMillis(1000), 
					    			 new Runnable() {
					            @Override
					            public void run() {
					            	reloadPallet();
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
    	log.debug("Auto Reloading Pallet");
    	if (isInputStation) {
    		serverSide.tell(StateOverrideRequests.SetLoaded, self); 
    	} else {
    		serverSide.tell(StateOverrideRequests.SetEmpty, self); 
    	}
	}
	
}
