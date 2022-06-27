package fiab.core.capabilities.handshake.server;

import fiab.core.capabilities.functionalunit.FURequest;

public class StartHandoverRequest extends FURequest {

    public StartHandoverRequest(String senderId) {
        super(senderId);
    }
}
