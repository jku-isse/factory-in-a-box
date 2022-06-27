package fiab.conveyor;

import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.functionalunit.BasicFUBehaviour;

public interface ConveyorCapability extends BasicFUBehaviour, OPCUABasicMachineBrowsenames{
    String CAPABILITY_ID = "DefaultConveyingCapability";

    String OPC_UA_BASE_URI = "http://factory-in-a-box.fiab/capabilities/transport/conveying";

    String LOAD_REQUEST = "LOAD";
    String UNLOAD_REQUEST = "UNLOAD";

    String CONVEYOR_ID = "ConveyorFU";

    void loadConveyor();

    void unloadConveyor();
}
