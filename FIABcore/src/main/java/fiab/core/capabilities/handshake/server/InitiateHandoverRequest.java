package fiab.core.capabilities.handshake.server;

import fiab.core.capabilities.functionalunit.FURequest;

public class InitiateHandoverRequest extends FURequest {

    public InitiateHandoverRequest(String senderId) {
        super(senderId);
    }
}
