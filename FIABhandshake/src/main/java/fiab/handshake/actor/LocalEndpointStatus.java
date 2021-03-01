package fiab.handshake.actor;

import akka.actor.ActorRef;
import fiab.core.capabilities.handshake.HandshakeCapability.ClientSideStates;
import fiab.core.capabilities.handshake.HandshakeCapability.ServerSideStates;
import fiab.tracing.actor.messages.TracingHeader;

public abstract class LocalEndpointStatus implements TracingHeader {
	private ActorRef actor;
	private boolean isProvidedCapability;
	private String capabilityId;
	private String header;

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
		this(actor, isProvidedCapability, capabilityId, "");
	}

	public LocalEndpointStatus(ActorRef actor, boolean isProvidedCapability, String capabilityId, String header) {
		super();
		this.actor = actor;
		this.isProvidedCapability = isProvidedCapability;
		this.capabilityId = capabilityId;
		this.header = header;
	}

	@Override
	public String getHeader() {
		return header;
	}

	@Override
	public void setHeader(String header) {
		this.header = header;
	}

	public abstract String getRawState();

	public static class LocalServerEndpointStatus extends LocalEndpointStatus {

		private ServerSideStates state = ServerSideStates.STOPPED;

		public LocalServerEndpointStatus(ActorRef actor, boolean isProvidedCapability, String capabilityId) {
			super(actor, isProvidedCapability, capabilityId);
		}

		public ServerSideStates getState() {
			return state;
		}

		public void setState(ServerSideStates state) {
			this.state = state;
		}

		@Override
		public String getRawState() {
			return state.toString();
		}
	}

	public static class LocalClientEndpointStatus extends LocalEndpointStatus {

		private ClientSideStates state = ClientSideStates.STOPPED;

		public LocalClientEndpointStatus(ActorRef actor, boolean isProvidedCapability, String capabilityId) {
			super(actor, isProvidedCapability, capabilityId);
		}

		public ClientSideStates getState() {
			return state;
		}

		public void setState(ClientSideStates state) {
			this.state = state;
		}

		@Override
		public String getRawState() {
			return state.toString();
		}
	}
}
