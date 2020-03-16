package fiab.mes.mockactors.transport;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.machine.msg.MachineStatus;
import fiab.mes.mockactors.transport.LocalEndpointStatus.LocalClientEndpointStatus;
import fiab.mes.mockactors.transport.LocalEndpointStatus.LocalServerEndpointStatus;
import fiab.mes.transport.actor.transportmodule.WellknownTransportModuleCapability;
import fiab.mes.transport.handshake.HandshakeProtocol.ClientMessageTypes;
import fiab.mes.transport.handshake.HandshakeProtocol.ClientSide;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerMessageTypes;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerSide;
import fiab.mes.transport.msg.InternalTransportModuleRequest;

public class NoOpTransportModuleWrapper extends AbstractActor{

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	protected ActorRef self;
	protected MachineStatus currentState = MachineStatus.UNKNOWN;
	protected HandshakeEndpointInfo eps;
	
	static public Props props() {	    
		return Props.create(NoOpTransportModuleWrapper.class, () -> new NoOpTransportModuleWrapper());
	}
	
	public NoOpTransportModuleWrapper() {
		self = getSelf();
		eps = new HandshakeEndpointInfo(self);
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(WellknownTransportModuleCapability.SimpleMessageTypes.class, msg -> {
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
				.match(ServerSide.class, state -> {
					// ignoring events from Handshake FUs
				})
				.match(ClientSide.class, state -> {										
					// ignoring events from Handshake FUs
				})				
				.build();
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
