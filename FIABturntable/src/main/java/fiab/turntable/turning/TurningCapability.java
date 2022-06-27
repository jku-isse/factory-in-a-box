package fiab.turntable.turning;

import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.functionalunit.BasicFUBehaviour;
import fiab.turntable.turning.messages.TurnRequest;

public interface TurningCapability extends BasicFUBehaviour, OPCUABasicMachineBrowsenames {

    String CAPABILITY_ID = "DefaultTurningCapability";

    String OPC_UA_BASE_URI = "http://factory-in-a-box.fiab/capabilities/transport/turning";

    String TURN_TO_REQUEST = "TURN_TO";

    String TURNING_ID = "TurningFU";

    void turnTo(TurnRequest request);
}
