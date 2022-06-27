package fiab.handshake.client.messages;

import fiab.core.capabilities.functionalunit.FURequest;
import fiab.core.capabilities.wiring.WiringInfo;

public class WiringRequest extends FURequest {

    private final WiringInfo info;

    public WiringRequest(String senderId, WiringInfo info) {
        super(senderId);
        this.info = info;
    }

    public WiringInfo getInfo() {
        return info;
    }
}
