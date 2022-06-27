package fiab.core.capabilities.handshake.client;

import fiab.core.capabilities.functionalunit.FURequest;

public class CompleteHandshake extends FURequest {
    public CompleteHandshake(String senderId) {
        super(senderId);
    }
}
