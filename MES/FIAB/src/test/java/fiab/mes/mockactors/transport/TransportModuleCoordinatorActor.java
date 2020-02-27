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
import fiab.mes.eventbus.SubscriptionClassifier;
import fiab.mes.machine.actor.WellknownMachinePropertyFields;
import fiab.mes.machine.msg.GenericMachineRequests;
import fiab.mes.machine.msg.MachineInWrongStateResponse;
import fiab.mes.machine.msg.MachineStatus;
import fiab.mes.machine.msg.MachineStatusUpdateEvent;
import fiab.mes.mockactors.MockServerHandshakeActor.StateOverrideRequests;
import fiab.mes.transport.actor.transportmodule.WellknownTransportModuleCapability;
import fiab.mes.transport.handshake.HandshakeProtocol;
import fiab.mes.transport.handshake.HandshakeProtocol.ClientMessageTypes;
import fiab.mes.transport.handshake.HandshakeProtocol.ClientSide;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerMessageTypes;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerSide;
import fiab.mes.transport.msg.InternalTransportModuleRequest;
import stateMachines.turning.TurnRequest;
import stateMachines.turning.TurnTableOrientation;
import stateMachines.turning.TurningStates;
import stateMachines.turning.TurntableStatusUpdateEvent;
import stateMachines.conveyor.ConveyorStates;
import stateMachines.conveyor.ConveyorStatusUpdateEvent;
import stateMachines.conveyor.ConveyorTriggers;

public class TransportModuleCoordinatorActor extends AbstractActor{

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	protected InterMachineEventBus interEventBus;
	protected boolean doPublishState = false;
	protected ActorRef self;
	protected MachineStatus currentState = MachineStatus.STOPPED;
	protected HandshakeEndpointInfo eps;

	protected ActorRef turntableFU;
	protected TurningStates ttFUState = TurningStates.STOPPED;
	protected ActorRef conveyorFU;
	protected ConveyorStates convFUState = ConveyorStates.STOPPED;
	protected InternalProcess exeSubState = InternalProcess.NOPROC;
	
	protected InternalTransportModuleRequest currentRequest;
	
	// we need to pass all actors representing server/client handshake and their capability ids
	static public Props props(InterMachineEventBus internalMachineEventBus, ActorRef turntableFU, ActorRef converyorFU) {	    
		return Props.create(TransportModuleCoordinatorActor.class, () -> new TransportModuleCoordinatorActor(internalMachineEventBus, turntableFU, converyorFU));
	}
	
	public TransportModuleCoordinatorActor(InterMachineEventBus machineEventBus, ActorRef turntableFU, ActorRef converyorFU) {
		this.interEventBus = machineEventBus;			
		self = getSelf();
		interEventBus.subscribe(self, new SubscriptionClassifier(self.path().name(), "*")); // to obtain events from turntable and conveyor
		eps = new HandshakeEndpointInfo(self);
		this.turntableFU = turntableFU;
		this.conveyorFU = converyorFU;
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
		        		turnToSource(req);
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
				.match(ServerSide.class, state -> {
					if (currentState.equals(MachineStatus.EXECUTE)) {
						String capId = getSender().path().name();
						log.info(String.format("ServerSide EP %s Status: %s", capId, state));
						eps.getHandshakeEP(capId).ifPresent(leps -> {
							((LocalServerEndpointStatus) leps).setState(state);
							if (state.equals(ServerSide.EXECUTE)) {
								if (exeSubState.equals(InternalProcess.HANDSHAKE_DEST))
									startLoadingOntoTurntable();
								else
									startUnloadingFromTurntable();
							}
//							if (state.equals(ServerSide.COMPLETING)) // superseeded by converyor signal
//								handleCompletingStateUpdate(capId);
						});
					} else {
						log.warning(String.format("Received ServerSide Event %s from %s in non EXECUTE state: %s", state, getSender().path().name(), currentState));
					}
				})
				.match(ClientSide.class, state -> {										
					if (currentState.equals(MachineStatus.EXECUTE)) {
						String capId = getSender().path().name();
						log.info(String.format("ClientSide EP %s local status: %s", capId, state));
						String localCapId = capId.lastIndexOf("~") > 0 ? capId.substring(0, capId.lastIndexOf("~")) : capId;

						eps.getHandshakeEP(localCapId).ifPresent(leps -> {
							((LocalClientEndpointStatus) leps).setState(state);
							switch(state) {
							case EXECUTE:
								if (exeSubState.equals(InternalProcess.HANDSHAKE_SOURCE))
									startLoadingOntoTurntable();
								else if (exeSubState.equals(InternalProcess.HANDSHAKE_DEST))
									startUnloadingFromTurntable();
								break;
//							case COMPLETING: // superseeded by Conveyor Signal
//								handleCompletingStateUpdate(localCapId);
//								break;
							case IDLE:
								getSender().tell(HandshakeProtocol.ClientMessageTypes.Start, self);
								break;
							default:
								break;
							}	
						}); 					
					} else {
						log.warning(String.format("Received ClientSide Event %s from %s in non EXECUTE state: %s, ignoring",state, getSender().path().name(), currentState));
					}
				})		
				.match(TurntableStatusUpdateEvent.class, state -> {
					ttFUState = state.getStatus();
					switch(state.getStatus()) {								
					case COMPLETING:
						// signal that handshake can start
						if (exeSubState.equals(InternalProcess.TURNING_SOURCE)){
							startSourceHandshake();
						} else if (exeSubState.equals(InternalProcess.TURNING_DEST)) {
							continueWithDestHandshake();
						}
						break;					
					case STOPPED:						
					case STOPPING:
						//TODO: how we handle if FU stops, and we are not in stopping up here
						break;
					}
				})
				.match(ConveyorStatusUpdateEvent.class, state -> {
					convFUState = state.getStatus();
					if (state.getStatus().equals(ConveyorStates.FULLY_OCCUPIED) && exeSubState.equals(InternalProcess.CONVEYING_SOURCE)) {
						Optional<LocalEndpointStatus> fromEP = eps.getHandshakeEP(currentRequest.getCapabilityInstanceIdFrom()); 
						fromEP.ifPresent(ep -> {
							if (ep.isProvidedCapability()) {
								ep.getActor().tell(HandshakeProtocol.ServerMessageTypes.Complete, self);
							} else {
								ep.getActor().tell(HandshakeProtocol.ClientMessageTypes.Complete, self);
							}
						});
						turnToDestination();
					} else if (state.getStatus().equals(ConveyorStates.IDLE) && exeSubState.equals(InternalProcess.CONVEYING_DEST)) {
						Optional<LocalEndpointStatus> toEP = eps.getHandshakeEP(currentRequest.getCapabilityInstanceIdTo()); 
						toEP.ifPresent(ep -> {
							if (ep.isProvidedCapability()) {
								ep.getActor().tell(HandshakeProtocol.ServerMessageTypes.Complete, self);
							} else {
								ep.getActor().tell(HandshakeProtocol.ClientMessageTypes.Complete, self);
							}
						});
						finalizeTransport();
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
		turntableFU.tell(new GenericMachineRequests.Reset(""), self);
		conveyorFU.tell(ConveyorTriggers.RESET, self);
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
            	if (ttFUState.equals(TurningStates.IDLE) && convFUState.equals(ConveyorStates.IDLE)) {
            		setAndPublishState(MachineStatus.IDLE); // only if FUs are also resetted to Idle can we also go into Idle
            	} else {
            		log.info("Waiting for FUs to turn IDLE before becoming IDLE oneself");
            		transitionResettingToIdle();
            	}
            }
          }, context().system().dispatcher());
	}
	
	private void turnToSource(InternalTransportModuleRequest req) {
		log.info("Starting Transport");
		currentRequest = req;
		setAndPublishState(MachineStatus.STARTING);
		// check which two handshake FUs we use,
		Optional<LocalEndpointStatus> fromEP = eps.getHandshakeEP(req.getCapabilityInstanceIdFrom()); 
		Optional<LocalEndpointStatus> toEP = eps.getHandshakeEP(req.getCapabilityInstanceIdTo());
		if (fromEP.isPresent() && toEP.isPresent()) {
			setAndPublishState(MachineStatus.EXECUTE);		
			turntableFU.tell(new TurnRequest(resolveCapabilityToOrientation(fromEP.get())), self);
			exeSubState = InternalProcess.TURNING_SOURCE;
		} else {
			if (!fromEP.isPresent())
				log.warning("Unknown HandshakeEndpoint identified by CapabilityId "+req.getCapabilityInstanceIdFrom());
			if (!toEP.isPresent())
				log.warning("Unknown HandshakeEndpoint identified by CapabilityId "+req.getCapabilityInstanceIdTo());
			stop();
		}										
	}
	
	private TurnTableOrientation resolveCapabilityToOrientation(LocalEndpointStatus les) {		
		if (les.getCapabilityId().startsWith("NORTH"))
			return TurnTableOrientation.NORTH;
		else if (les.getCapabilityId().startsWith("SOUTH"))
			return TurnTableOrientation.SOUTH;
		else if (les.getCapabilityId().startsWith("WEST"))
			return TurnTableOrientation.WEST;
		else if (les.getCapabilityId().startsWith("EAST"))
			return TurnTableOrientation.EAST;
		else
			log.error("Cannot resolve CapabilityId to TurntableOrientation: "+les.getCapabilityId());
		return null; //TODO: better handling than via null needed
	}
	
	private void startSourceHandshake() {		
		Optional<LocalEndpointStatus> fromEP = eps.getHandshakeEP(currentRequest.getCapabilityInstanceIdFrom()); 
		Optional<LocalEndpointStatus> toEP = eps.getHandshakeEP(currentRequest.getCapabilityInstanceIdTo());
		if (fromEP.isPresent() && toEP.isPresent()) {
			fromEP.ifPresent(leps -> {
				exeSubState = InternalProcess.HANDSHAKE_SOURCE;
				// now check if localEP is client or server, then reset
				if (leps.isProvidedCapability()) {
					leps.getActor().tell(HandshakeProtocol.ServerMessageTypes.Reset, self);
				} else {
					leps.getActor().tell(HandshakeProtocol.ClientMessageTypes.Reset, self);
				}
			});
		}			
	}
	
	private void startLoadingOntoTurntable() {
		log.info("Starting to load turntable");
		exeSubState = InternalProcess.CONVEYING_SOURCE;
		conveyorFU.tell(ConveyorTriggers.LOAD, self);
		
	}
	
//	private void handleCompletingStateUpdate(String capId) {
//		try {
//		if (currentRequest.getCapabilityInstanceIdFrom().equals(capId)) { // first finished
//			continueWithDestHandshake();
//		} else if (currentRequest.getCapabilityInstanceIdTo().equals(capId)) { //second finished
//			finalizeTransport();
//		}
//		} catch(Exception e) {
//			// woops
//			System.out.println(capId);
//		}
//	}
	
	private void turnToDestination() {
		log.info("Starting to Turn to Destination");
		Optional<LocalEndpointStatus> toEP = eps.getHandshakeEP(currentRequest.getCapabilityInstanceIdTo());
		toEP.ifPresent(ep -> {
			turntableFU.tell(new TurnRequest(resolveCapabilityToOrientation(ep)), self);
			exeSubState = InternalProcess.TURNING_DEST;
		});
	}
	
	private void continueWithDestHandshake() {		
		log.info("Continuing with Destination Handshake via: "+currentRequest.getCapabilityInstanceIdTo());
		// imitate turning towards second
		Optional<LocalEndpointStatus> toEP = eps.getHandshakeEP(currentRequest.getCapabilityInstanceIdTo());
		// now check if localEP is client or server
		// reset the second,
		toEP.ifPresent(leps -> {
			// now check if localEP is client or server, then reset
			exeSubState = InternalProcess.HANDSHAKE_DEST;
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
	
	private void startUnloadingFromTurntable() {
		log.info("Starting to unload turntable");
		conveyorFU.tell(ConveyorTriggers.UNLOAD, self);
		exeSubState = InternalProcess.CONVEYING_DEST;
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
		exeSubState = InternalProcess.NOPROC;
		//tell all handshake FUs to stop, we ignore HandshakeFU level for now
		eps.tellAllEPsToStop();
		// serverSide.tell(MockServerHandshakeActor.MessageTypes.Stop, getSelf());
		turntableFU.tell(new GenericMachineRequests.Stop(""), self);
		conveyorFU.tell(ConveyorTriggers.STOP, self);
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
	
	private static enum InternalProcess {
		TURNING_SOURCE, HANDSHAKE_SOURCE, CONVEYING_SOURCE, TURNING_DEST, HANDSHAKE_DEST, CONVEYING_DEST, NOPROC
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
						les.getActor().tell(ServerMessageTypes.Stop, self);
					} else {
						les.getActor().tell(ClientMessageTypes.Stop, self);
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
		
		private ServerSide state = ServerSide.STOPPED;
		
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
		
		private ClientSide state = ClientSide.STOPPED;
		
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
