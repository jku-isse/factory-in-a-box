package actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.google.common.collect.Sets;
import event.MachineStatusUpdateEvent;
import event.bus.InterMachineEventBus;
import event.bus.WellknownMachinePropertyFields;
import event.capability.WellknownTransportModuleCapability;
import handshake.HandshakeProtocol;
import msg.InternalTransportModuleRequest;
import msg.MachineInWrongStateResponse;
import msg.StateOverrideRequests;
import stateMachines.MachineStatus;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class TransportModuleWrapper extends AbstractActor{

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	protected InterMachineEventBus interEventBus;
	protected boolean doPublishState = false;
	protected ActorRef self;
	protected MachineStatus currentState = MachineStatus.STOPPED;
	protected HandshakeEndpointInfo eps;

	protected InternalTransportModuleRequest currentRequest;
	
	// we need to pass all actors representing server/client handshake and their capability ids
	static public Props props(InterMachineEventBus internalMachineEventBus) {	    
		return Props.create(TransportModuleWrapper.class, () -> new TransportModuleWrapper(internalMachineEventBus));
	}
	
	public TransportModuleWrapper(InterMachineEventBus machineEventBus) {
		this.interEventBus = machineEventBus;		
		self = getSelf();
		eps = new HandshakeEndpointInfo(self);
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(WellknownTransportModuleCapability.SimpleMessageTypes.class, msg -> {
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
						setAndPublishState(currentState);
						break;
					default:
						break;
					}
				})
//				.match(HandshakeEndpointInfo.class, eps -> { // depricated
//					if (!epNonUpdateableStates.contains(currentState)) {
//						this.eps = eps;						
//					} else {
//						log.warning("Trying to update Handshake Endpoints in nonupdateable state: "+currentState);
//					}						
//				})
				.match(LocalClientEndpointStatus.class, les -> {
					if (!epNonUpdateableStates.contains(currentState)) {
						this.eps.addOrReplace(les);						
					} else {
						log.warning("Trying to update Handshake Endpoints in nonupdateable state: "+currentState);
					}
				})
				.match(LocalServerEndpointStatus.class, les -> {
					if (!epNonUpdateableStates.contains(currentState)) {
						this.eps.addOrReplace(les);						
					} else {
						log.warning("Trying to update Handshake Endpoints in nonupdateable state: "+currentState);
					}
				})
				.match(InternalTransportModuleRequest.class, req -> {
					if (currentState.equals(MachineStatus.IDLE)) {
						sender().tell(new MachineStatusUpdateEvent(self.path().name(), null, WellknownMachinePropertyFields.STATE_VAR_NAME, "", MachineStatus.STARTING), self);
		        		startTransport(req);
					} else {
		        		log.warning("Received TransportModuleRequest in incompatible state: "+currentState);
						//respond with error message that we are not in the right state for request
		        		sender().tell(new MachineInWrongStateResponse(getSelf().path().name(),
		        				WellknownMachinePropertyFields.STATE_VAR_NAME, 
		        				"Machine is not in correct state",
		        				currentState,
		        				req,
		        				MachineStatus.IDLE), self);
		        	}
				})
				.match(HandshakeProtocol.ServerSide.class, state -> {
					if (currentState.equals(MachineStatus.EXECUTE)) {
						String capId = getSender().path().name();
						log.info(String.format("ServerSide EP %s Status: %s", capId, state));
						eps.getHandshakeEP(capId).ifPresent(leps -> {
							((LocalServerEndpointStatus) leps).setState(state);
							if (state.equals(HandshakeProtocol.ServerSide.COMPLETING))
								handleCompletingStateUpdate(capId);
						});
					} else {
						log.warning(String.format("Received ServerSide Event %s from %s in non EXECUTE state: %s", state, getSender().path().name(), currentState));
					}
				})
				.match(HandshakeProtocol.ClientSide.class, state -> {
					if (currentState.equals(MachineStatus.EXECUTE)) {
						String capId = getSender().path().name();
						log.info(String.format("ClientSide EP %s local status: %s", capId, state));
						String localCapId = capId.lastIndexOf("~") > 0 ? capId.substring(0, capId.lastIndexOf("~")) : capId;

						eps.getHandshakeEP(localCapId).ifPresent(leps -> {
							((LocalClientEndpointStatus) leps).setState(state);
							switch(state) {
							case COMPLETING:
								handleCompletingStateUpdate(localCapId);
								break;
							case IDLE:
								getSender().tell(HandshakeProtocol.ClientMessageTypes.Start, self);
								break;
							default:
								break;
							}	
						}); 					
					} else {
						log.info(String.format("Received ClientSide Event %s from %s in non EXECUTE state: %s, ignoring",state, getSender().path().name(), currentState));
					}
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
		// EPS are reset upon transport start
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
					leps.getActor().tell(HandshakeProtocol.ServerMessageTypes.Reset, self);
				} else {
					leps.getActor().tell(HandshakeProtocol.ClientMessageTypes.Reset, self);
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
		try {
		if (currentRequest.getCapabilityInstanceIdFrom().equals(capId)) { // first finished
			continueTransport();
		} else if (currentRequest.getCapabilityInstanceIdTo().equals(capId)) { //second finished
			finalizeTransport();
		}
		} catch(Exception e) {
			// woops
			System.out.println(capId);
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
				// as the second transport part, the this server/turntable has to be loaded
				leps.getActor().tell(StateOverrideRequests.SetLoaded, self);
				leps.getActor().tell(HandshakeProtocol.ServerMessageTypes.Reset, self);
			} else {
				leps.getActor().tell(HandshakeProtocol.ClientMessageTypes.Reset, self);
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
		//tell all handshake FUs to stop, we ignore HandshakeFU level for now
		eps.tellAllEPsToStop();
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
	
	private static class HandshakeEndpointInfo {
		protected Map<String,LocalEndpointStatus> handshakeEPs = new HashMap<>();
		
		ActorRef self;
		protected HandshakeEndpointInfo(ActorRef self) {
			this.self = self;
		}
		
//		public HandshakeEndpointInfo(Map<String,LocalEndpointStatus> handshakeEPs) {
//			this.handshakeEPs = handshakeEPs;
//		}
		
		public Optional<LocalEndpointStatus> getHandshakeEP(String capabilityId) {			
			if (capabilityId != null && handshakeEPs.containsKey(capabilityId))
				return Optional.ofNullable(handshakeEPs.get(capabilityId));
			else
				return Optional.empty();
		}			
		
		public void addOrReplace(LocalEndpointStatus les) {
			handshakeEPs.put(les.getCapabilityId(), les);
		}
		
		public void tellAllEPsToStop() {
			handshakeEPs.values().stream()
				.forEach(les -> {
					if (les.isProvidedCapability) { 					// if server use server msg
						les.getActor().tell(HandshakeProtocol.ServerMessageTypes.Stop, self);
					} else {
						les.getActor().tell(HandshakeProtocol.ClientMessageTypes.Stop, self);
					}			
				});
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
		
		private HandshakeProtocol.ServerSide state = HandshakeProtocol.ServerSide.STOPPED;
		
		public LocalServerEndpointStatus(ActorRef actor, boolean isProvidedCapability, String capabilityId) {
			super(actor, isProvidedCapability, capabilityId);			
		}				
		
		public HandshakeProtocol.ServerSide getState() {
			return state;
		}
		public void setState(HandshakeProtocol.ServerSide state) {
			this.state = state;
		}

		@Override
		public String getRawState() {
			return state.toString();
		}
	}
	
	public static class LocalClientEndpointStatus extends LocalEndpointStatus{
		
		private HandshakeProtocol.ClientSide state = HandshakeProtocol.ClientSide.STOPPED;
		
		public LocalClientEndpointStatus(ActorRef actor, boolean isProvidedCapability, String capabilityId) {
			super(actor, isProvidedCapability, capabilityId);			
		}				
		
		public HandshakeProtocol.ClientSide getState() {
			return state;
		}
		public void setState(HandshakeProtocol.ClientSide state) {
			this.state = state;
		}
		@Override
		public String getRawState() {
			return state.toString();
		}
	}
}
