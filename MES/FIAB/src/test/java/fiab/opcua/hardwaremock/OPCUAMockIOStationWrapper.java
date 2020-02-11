package fiab.opcua.hardwaremock;

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
import fiab.mes.mockactors.iostation.MockInputStationServerHandshakeActor;
import fiab.mes.mockactors.iostation.MockOutputStationServerHandshakeActor;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerSide;

public class OPCUAMockIOStationWrapper extends AbstractActor {

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	protected boolean doPublishState = true;
	protected ServerSide handshakeStatus = ServerSide.STOPPED;
	protected ActorRef serverSide;
	protected ActorRef self;
	protected boolean isInputStation = true;
	protected boolean doAutoReload;
	protected StatePublisher spub;
	
	static public Props props(boolean isInputStation, boolean doAutoReload) {	    
		return Props.create(OPCUAMockIOStationWrapper.class, () -> new OPCUAMockIOStationWrapper(isInputStation, doAutoReload));
	}
	
	public OPCUAMockIOStationWrapper(boolean isInputStation,boolean doAutoReload) {
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
						if (msg.equals(ServerSide.COMPLETED)) { //we auto reload here
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

	private void setAndPublishState(ServerSide newState) {
		if (doPublishState && spub != null) {
			//interEventBus.publish(new IOStationStatusUpdateEvent(self.path().toString(), "", newState));
			spub.setStatusValue(newState.toString());
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
