package fiab.conveyor.messages;

import fiab.core.capabilities.functionalunit.FURequest;

public class UnloadConveyorRequest extends FURequest {
    public UnloadConveyorRequest(String senderId) {
        super(senderId);
    }
}
