package fiab.handshake.actor;

import akka.actor.ActorRef;
import fiab.core.capabilities.handshake.ClientSideStates;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.functionalunit.connector.FUConnector;


public abstract class LocalEndpointStatus {

	private final boolean isProvidedCapability;
	private final String capabilityId;

	public LocalEndpointStatus(boolean isProvidedCapability, String capabilityId) {
		super();
		this.isProvidedCapability = isProvidedCapability;
		this.capabilityId = capabilityId;
	}

	public boolean isProvidedCapability() {
		return isProvidedCapability;
	}
	public String getCapabilityId() {
		return capabilityId;
	}
	public abstract String getRawState();

	public static class LocalServerEndpointStatus extends LocalEndpointStatus{

		private ServerSideStates state = ServerSideStates.STOPPED;

		public LocalServerEndpointStatus(String capabilityId) {
			super( true, capabilityId);
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

	public static class LocalClientEndpointStatus extends LocalEndpointStatus{

		private ClientSideStates state = ClientSideStates.STOPPED;

		public LocalClientEndpointStatus(String capabilityId) {
			super(false, capabilityId);
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
