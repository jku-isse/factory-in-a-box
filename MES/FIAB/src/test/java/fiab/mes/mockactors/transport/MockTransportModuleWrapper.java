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
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.handshake.IOStationCapability;
import fiab.core.capabilities.handshake.HandshakeCapability.ClientMessageTypes;
import fiab.core.capabilities.handshake.HandshakeCapability.ClientSide;
import fiab.core.capabilities.handshake.HandshakeCapability.ServerMessageTypes;
import fiab.core.capabilities.handshake.HandshakeCapability.ServerSide;
import fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers;
import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.machine.msg.MachineInWrongStateResponse;
import fiab.mes.machine.msg.MachineStatusUpdateEvent;
import fiab.mes.mockactors.MockServerHandshakeActor.StateOverrideRequests;
import fiab.mes.mockactors.transport.LocalEndpointStatus.LocalClientEndpointStatus;
import fiab.mes.mockactors.transport.LocalEndpointStatus.LocalServerEndpointStatus;
import fiab.mes.transport.msg.InternalTransportModuleRequest;

public class MockTransportModuleWrapper extends AbstractActor{

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	protected InterMachineEventBus interEventBus;
	protected boolean doPublishState = false;
	protected ActorRef self;
	protected BasicMachineStates currentState = BasicMachineStates.STOPPED;
	protected HandshakeEndpointInfo eps;

	protected InternalTransportModuleRequest currentRequest;
	
	// we need to pass all actors representing server/client handshake and their capability ids
	static public Props props(InterMachineEventBus internalMachineEventBus) {	    
		return Props.create(MockTransportModuleWrapper.class, () -> new MockTransportModuleWrapper(internalMachineEventBus));
	}
	
	public MockTransportModuleWrapper(InterMachineEventBus machineEventBus) {
		this.interEventBus = machineEventBus;		
		self = getSelf();
		eps = new HandshakeEndpointInfo(self);
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.class, msg -> {
					switch(msg) {
					case Reset:
						if (currentState.equals(BasicMachineStates.STOPPED) || currentState.equals(BasicMachineStates.COMPLETE))
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
					if (currentState.equals(BasicMachineStates.IDLE)) {
						sender().tell(new MachineStatusUpdateEvent(self.path().name(), null, OPCUABasicMachineBrowsenames.STATE_VAR_NAME, "", BasicMachineStates.STARTING), self);
		        		startTransport(req);
					} else {
		        		log.warning("Received TransportModuleRequest in incompatible state: "+currentState);
						//respond with error message that we are not in the right state for request
		        		sender().tell(new MachineInWrongStateResponse(getSelf().path().name(), 
		        				OPCUABasicMachineBrowsenames.STATE_VAR_NAME, 
		        				"Machine is not in correct state",
		        				currentState,
		        				req,
		        				BasicMachineStates.IDLE), self);
		        	}
				})
				.match(ServerSide.class, state -> {
					if (currentState.equals(BasicMachineStates.EXECUTE)) {
						String capId = getSender().path().name();
						log.info(String.format("ServerSide EP %s Status: %s", capId, state));
						eps.getHandshakeEP(capId).ifPresent(leps -> {
							((LocalServerEndpointStatus) leps).setState(state);
							if (state.equals(ServerSide.COMPLETING))
								handleCompletingStateUpdate(capId);
						});
					} else {
						log.warning(String.format("Received ServerSide Event %s from %s in non EXECUTE state: %s", state, getSender().path().name(), currentState));
					}
				})
				.match(ClientSide.class, state -> {										
					if (currentState.equals(BasicMachineStates.EXECUTE)) {
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
								getSender().tell(IOStationCapability.ClientMessageTypes.Start, self);
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

	private Set<BasicMachineStates> epNonUpdateableStates = Sets.immutableEnumSet(BasicMachineStates.STARTING, BasicMachineStates.EXECUTE, BasicMachineStates.COMPLETING);
	
	protected void setAndPublishState(BasicMachineStates newState) {
		//log.debug(String.format("%s sets state from %s to %s", this.machineId.getId(), this.currentState, newState));
		this.currentState = newState;
		if (doPublishState) {
			interEventBus.publish(new MachineStatusUpdateEvent(self.path().name(), null, OPCUABasicMachineBrowsenames.STATE_VAR_NAME, "", newState));
		}
	}
	
	private void reset() {
		setAndPublishState(BasicMachineStates.RESETTING);
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
            	setAndPublishState(BasicMachineStates.IDLE); 
            }
          }, context().system().dispatcher());
	}
	
	private void startTransport(InternalTransportModuleRequest req) {
		log.info("Starting Transport");
		currentRequest = req;
		setAndPublishState(BasicMachineStates.STARTING);
		// check which two handshake FUs we use,
		Optional<LocalEndpointStatus> fromEP = eps.getHandshakeEP(req.getCapabilityInstanceIdFrom()); 
		Optional<LocalEndpointStatus> toEP = eps.getHandshakeEP(req.getCapabilityInstanceIdTo());
		if (fromEP.isPresent() && toEP.isPresent()) {
			setAndPublishState(BasicMachineStates.EXECUTE);
			// imitate turning towards first
			fromEP.ifPresent(leps -> {
				// now check if localEP is client or server, then reset
				if (leps.isProvidedCapability()) {
					leps.getActor().tell(IOStationCapability.ServerMessageTypes.Reset, self);
				} else {
					leps.getActor().tell(IOStationCapability.ClientMessageTypes.Reset, self);
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
				leps.getActor().tell(IOStationCapability.ServerMessageTypes.Reset, self);
			} else {
				leps.getActor().tell(IOStationCapability.ClientMessageTypes.Reset, self);
			}
		});
		// when execute, immitate loading, complete second		
	}
	
	private void finalizeTransport() {
		log.info("Finalizing Transport");
		// transition into Completing, handshakes should be now in complete as well
		setAndPublishState(BasicMachineStates.COMPLETING);
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(500), 
    			 new Runnable() {
            @Override
            public void run() {            	
            	setAndPublishState(BasicMachineStates.COMPLETE);
            	//we do autoresetting here
            	reset();           	
            }
          }, context().system().dispatcher());
	}
	

	private void stop() {
		setAndPublishState(BasicMachineStates.STOPPING);
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
		setAndPublishState(BasicMachineStates.STOPPED); 
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
					if (les.isProvidedCapability()) { 					// if server use server msg
						les.getActor().tell(ServerMessageTypes.Stop, self);
					} else {
						les.getActor().tell(ClientMessageTypes.Stop, self);
					}			
				});
		}
		
	}
}
