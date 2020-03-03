package handshake;

import akka.actor.ActorRef;

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


    public static class LocalServerEndpointStatus extends LocalEndpointStatus {

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

    public static class LocalClientEndpointStatus extends LocalEndpointStatus {

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

