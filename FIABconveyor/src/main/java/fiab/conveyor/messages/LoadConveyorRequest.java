package fiab.conveyor.messages;

import fiab.core.capabilities.functionalunit.FURequest;

public class LoadConveyorRequest extends FURequest {

    public LoadConveyorRequest(String senderId) {
        super(senderId);
    }
}
