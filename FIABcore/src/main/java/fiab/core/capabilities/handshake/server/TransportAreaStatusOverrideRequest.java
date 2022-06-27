package fiab.core.capabilities.handshake.server;

import fiab.core.capabilities.functionalunit.FURequest;
import fiab.core.capabilities.handshake.HandshakeCapability;

public class TransportAreaStatusOverrideRequest extends FURequest {

    HandshakeCapability.StateOverrideRequests overrideRequest;

    public TransportAreaStatusOverrideRequest(String senderId, HandshakeCapability.StateOverrideRequests overrideRequest) {
        super(senderId);
        this.overrideRequest = overrideRequest;
    }

    public HandshakeCapability.StateOverrideRequests getOverrideRequest() {
        return overrideRequest;
    }
}
