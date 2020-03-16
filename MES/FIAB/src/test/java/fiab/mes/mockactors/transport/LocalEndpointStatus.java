package fiab.mes.mockactors.transport;

import akka.actor.ActorRef;
import fiab.mes.transport.handshake.HandshakeProtocol.ClientSide;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerSide;

public abstract class LocalEndpointStatus {
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
