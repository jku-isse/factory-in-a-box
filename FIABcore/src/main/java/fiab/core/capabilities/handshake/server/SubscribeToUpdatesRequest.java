package fiab.core.capabilities.handshake.server;

import fiab.core.capabilities.functionalunit.FURequest;

public class SubscribeToUpdatesRequest extends FURequest {

    public SubscribeToUpdatesRequest(String senderId) {
        super(senderId);
    }
}
