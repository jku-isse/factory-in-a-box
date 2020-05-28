package fiab.turntable.actor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.handshake.HandshakeCapability.ClientMessageTypes;
import fiab.core.capabilities.handshake.HandshakeCapability.ClientSideStates;
import fiab.core.capabilities.handshake.HandshakeCapability.ServerMessageTypes;
import fiab.core.capabilities.handshake.HandshakeCapability.ServerSideStates;
import fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers;
//import fiab.handshake.actor.LocalEndpointStatus;
import fiab.handshake.actor.LocalEndpointStatus;
import fiab.handshake.actor.LocalEndpointStatus.LocalClientEndpointStatus;
import fiab.handshake.actor.LocalEndpointStatus.LocalServerEndpointStatus;

public class NoOpTransportModuleCoordinator extends AbstractActor{

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	protected ActorRef self;
	protected BasicMachineStates currentState = BasicMachineStates.UNKNOWN;
	protected HandshakeEndpointInfo eps;
	
	static public Props props() {	    
		return Props.create(NoOpTransportModuleCoordinator.class, () -> new NoOpTransportModuleCoordinator());
	}
	
	public NoOpTransportModuleCoordinator() {
		self = getSelf();
		eps = new HandshakeEndpointInfo(self);
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.class, msg -> {
					log.warning("Not supposed to get message of type: "+ msg.toString());
				})
				.match(LocalClientEndpointStatus.class, les -> {
						this.eps.addOrReplace(les);						
				})
				.match(LocalServerEndpointStatus.class, les -> {
						this.eps.addOrReplace(les);						
				})
				.match(InternalTransportModuleRequest.class, req -> {
					log.warning("Not supposed to get message of type: "+ req.toString());
				})
				.match(ServerSideStates.class, state -> {
					// ignoring events from Handshake FUs
				})
				.match(ClientSideStates.class, state -> {										
					// ignoring events from Handshake FUs
				})				
				.build();
	}
	
	private static class HandshakeEndpointInfo {
		protected Map<String, LocalEndpointStatus> handshakeEPs = new HashMap<>();
		
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
