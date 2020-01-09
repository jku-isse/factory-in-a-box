package fiab.mes.mockactors.transport;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.Sets;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.machine.actor.WellknownMachinePropertyFields;
import fiab.mes.machine.msg.MachineStatus;
import fiab.mes.machine.msg.MachineStatusUpdateEvent;
import fiab.mes.mockactors.MockClientHandshakeActor;
import fiab.mes.mockactors.MockServerHandshakeActor;
import fiab.mes.transport.handshake.HandshakeProtocol.ClientSide;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerSide;
import fiab.mes.transport.msg.InternalTransportModuleRequest;

public class MockTransportModuleWrapper extends AbstractActor{

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	protected InterMachineEventBus interEventBus;
	protected boolean doPublishState = false;
	protected ActorRef self;
	protected MachineStatus currentState = MachineStatus.STOPPED;
	protected HandshakeEndpointInfo eps = null;

	protected InternalTransportModuleRequest currentRequest;
	
	// we need to pass all actors representing server/client handshake and their capability ids
	static public Props props(InterMachineEventBus internalMachineEventBus) {	    
		return Props.create(MockTransportModuleWrapper.class, () -> new MockTransportModuleWrapper(internalMachineEventBus));
	}
	
	public MockTransportModuleWrapper(InterMachineEventBus machineEventBus) {
		this.interEventBus = machineEventBus;		
		self = getSelf();
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(SimpleMessageTypes.class, msg -> {
					switch(msg) {
					case Reset:
						if (currentState.equals(MachineStatus.STOPPED) || currentState.equals(MachineStatus.COMPLETE))
							reset();
						else 
							log.warning("Wrapper told to reset in wrong state "+currentState);
						break;
					case Stop:
						stop();
						break;
					case SubscribeState:
						doPublishState = true;
						break;
					default:
						break;
					}
				})
				.match(HandshakeEndpointInfo.class, eps -> {
					if (!epNonUpdateableStates.contains(currentState)) {
						this.eps = eps;						
					} else {
						log.warning("Trying to update Handshake Endpoints in nonupdateable state: "+currentState);
					}						
				})
				.match(InternalTransportModuleRequest.class, req -> {
					if (currentState.equals(MachineStatus.IDLE)) {
		        		startTransport(req);
					} else {
		        		log.warning("Received TransportModuleRequest in incompatible state: "+currentState);
						//TODO: respond with error message that we are not in the right state for request
		        	}
				})
				.match(ServerSide.class, state -> {
					String capId = getSender().path().name();
					log.info(String.format("ServerSide EP %s Status: %s", capId, state));
					eps.getHandshakeEP(capId).ifPresent(leps -> {
						((LocalServerEndpointStatus) leps).setState(state);
						if (state.equals(ServerSide.Completing))
							handleCompletingStateUpdate(capId);
					});
				})
				.match(ClientSide.class, state -> {
					String capId = getSender().path().name();
					log.info(String.format("ClientSide EP %s Status: %s", capId, state));
					String localCapId = capId.lastIndexOf("~") > 0 ?
						capId = capId.substring(0, capId.lastIndexOf("~")) : capId;
					eps.getHandshakeEP(localCapId).ifPresent(leps -> {
						((LocalClientEndpointStatus) leps).setState(state);
						switch(state) {
						case Completing:
							handleCompletingStateUpdate(localCapId);
							break;
						case Idle:
							getSender().tell(MockClientHandshakeActor.MessageTypes.Start, self);
							break;
						default:
							break;
						}	
					});
				})				
				.build();
	}

	private Set<MachineStatus> epNonUpdateableStates = Sets.immutableEnumSet(MachineStatus.STARTING, MachineStatus.EXECUTE, MachineStatus.COMPLETING);
	
	protected void setAndPublishState(MachineStatus newState) {
		//log.debug(String.format("%s sets state from %s to %s", this.machineId.getId(), this.currentState, newState));
		this.currentState = newState;
		if (doPublishState) {
			interEventBus.publish(new MachineStatusUpdateEvent(self.path().name(), null, WellknownMachinePropertyFields.STATE_VAR_NAME, "", newState));
		}
	}
	
	private void reset() {
		setAndPublishState(MachineStatus.RESETTING);
		currentRequest = null;
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000), 
    			 new Runnable() {
            @Override
            public void run() {
            	 transitionResettingToIdle();
            }
          }, context().system().dispatcher());
	}
	
	private void transitionResettingToIdle() {
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000), 
    			 new Runnable() {
            @Override
            public void run() {
            	setAndPublishState(MachineStatus.IDLE); 
            }
          }, context().system().dispatcher());
	}
	
	private void startTransport(InternalTransportModuleRequest req) {
		log.info("Starting Transport");
		currentRequest = req;
		setAndPublishState(MachineStatus.STARTING);
		// check which two handshake FUs we use,
		Optional<LocalEndpointStatus> fromEP = eps.getHandshakeEP(req.getCapabilityInstanceIdFrom()); 
		Optional<LocalEndpointStatus> toEP = eps.getHandshakeEP(req.getCapabilityInstanceIdTo());
		if (fromEP.isPresent() && toEP.isPresent()) {
			setAndPublishState(MachineStatus.EXECUTE);
			// imitate turning towards first
			fromEP.ifPresent(leps -> {
				// now check if localEP is client or server, then reset
				if (leps.isProvidedCapability()) {
					leps.getActor().tell(MockServerHandshakeActor.MessageTypes.Reset, self);
				} else {
					leps.getActor().tell(MockClientHandshakeActor.MessageTypes.Reset, self);
				}
			});			
			// when execute, immitate loading, complete first (not necessary with autocomplete
		} else {
			if (!fromEP.isPresent())
				log.warning("Unknown HandshakeEndpoint identified by CapabilityId "+req.getCapabilityInstanceIdFrom());
			if (!toEP.isPresent())
				log.warning("Unknown HandshakeEndpoint identified by CapabilityId "+req.getCapabilityInstanceIdTo());
			stop();
		}										
	}
	
	private void handleCompletingStateUpdate(String capId) {
		if (currentRequest.getCapabilityInstanceIdFrom().equals(capId)) // first finished
			continueTransport();
		else if (currentRequest.getCapabilityInstanceIdTo().equals(capId)) {
			finalizeTransport();
		}
	}
	
	private void continueTransport() {		
		log.info("Continuing Transport");
		// imitate turning towards second
		Optional<LocalEndpointStatus> toEP = eps.getHandshakeEP(currentRequest.getCapabilityInstanceIdTo());
		// now check if localEP is client or server
		// reset the second,
		toEP.ifPresent(leps -> {
			// now check if localEP is client or server, then reset
			if (leps.isProvidedCapability()) {
				leps.getActor().tell(MockServerHandshakeActor.MessageTypes.Reset, self);
			} else {
				leps.getActor().tell(MockClientHandshakeActor.MessageTypes.Reset, self);
			}
		});
		// when execute, immitate loading, complete second		
	}
	
	private void finalizeTransport() {
		log.info("Finalizing Transport");
		// transition into Completing, handshakes should be now in complete as well
		setAndPublishState(MachineStatus.COMPLETING);
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(500), 
    			 new Runnable() {
            @Override
            public void run() {            	
            	setAndPublishState(MachineStatus.COMPLETE);
            	//we do autoresetting here
            	reset();           	
            }
          }, context().system().dispatcher());
	}
	

	private void stop() {
		setAndPublishState(MachineStatus.STOPPING);
		//TODO tell all handshake FUs to stop, we ignore HandshakeFU level for now
		// serverSide.tell(MockServerHandshakeActor.MessageTypes.Stop, getSelf());
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000), 
    			 new Runnable() {
            @Override
            public void run() {
            	// only when handshakeFU and other FUs have stopped
            	//if (handshakeStatus.equals(ServerSide.Stopped)) {
            		transitionToStop();
            	//}
            }
          }, context().system().dispatcher());
	}
	
	private void transitionToStop() {
		setAndPublishState(MachineStatus.STOPPED); 
	}
	
	public static enum SimpleMessageTypes {
		SubscribeState, Reset, Stop
	}
	
	public static class HandshakeEndpointInfo {
		protected Map<String,LocalEndpointStatus> handshakeEPs = new HashMap<>();
		
		public HandshakeEndpointInfo(Map<String,LocalEndpointStatus> handshakeEPs) {
			this.handshakeEPs = handshakeEPs;
		}
		
		public Optional<LocalEndpointStatus> getHandshakeEP(String capabilityId) {
			return Optional.ofNullable(handshakeEPs.get(capabilityId));
		}			
	}
	
	public abstract static class LocalEndpointStatus {
		private ActorRef actor;		
		private boolean isProvidedCapability;
		private String capabilityId;
		
		public ActorRef getActor() {
			return actor;
		}
		public boolean isProvidedCapability() {
			return isProvidedCapability;
		}
		public String getCapabilityId() {
			return capabilityId;
		}
		public LocalEndpointStatus(ActorRef actor, boolean isProvidedCapability, String capabilityId) {
			super();
			this.actor = actor;
			this.isProvidedCapability = isProvidedCapability;
			this.capabilityId = capabilityId;
		}		
		public abstract String getRawState();
	}
	
	public static class LocalServerEndpointStatus extends LocalEndpointStatus{
		
		private ServerSide state = ServerSide.Stopped;
		
		public LocalServerEndpointStatus(ActorRef actor, boolean isProvidedCapability, String capabilityId) {
			super(actor, isProvidedCapability, capabilityId);			
		}				
		
		public ServerSide getState() {
			return state;
		}
		public void setState(ServerSide state) {
			this.state = state;
		}

		@Override
		public String getRawState() {
			return state.toString();
		}
	}
	
	public static class LocalClientEndpointStatus extends LocalEndpointStatus{
		
		private ClientSide state = ClientSide.Stopped;
		
		public LocalClientEndpointStatus(ActorRef actor, boolean isProvidedCapability, String capabilityId) {
			super(actor, isProvidedCapability, capabilityId);			
		}				
		
		public ClientSide getState() {
			return state;
		}
		public void setState(ClientSide state) {
			this.state = state;
		}
		@Override
		public String getRawState() {
			return state.toString();
		}
	}
}
