package fiab.core.capabilities.handshake.server;

import fiab.core.capabilities.functionalunit.FURequest;

public class UnsubscribeToUpdatesRequest extends FURequest {

    public UnsubscribeToUpdatesRequest(String senderId) {
        super(senderId);
    }
}
