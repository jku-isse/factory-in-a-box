package fiab.core.capabilities.buffer;

import ProcessCore.AbstractCapability;
import fiab.core.capabilities.ComparableCapability;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;

public interface BufferStationCapability extends OPCUABasicMachineBrowsenames {

    public static String OPCUA_LOAD_REQUEST = "LoadRequest";
    public static String OPCUA_UNLOAD_REQUEST = "UnloadRequest";

    public static String BUFFER_CAPABILITY_URI = "http://factory-in-a-box.fiab/capabilities/buffer";

    public static AbstractCapability getBufferCapability() {
        ComparableCapability ac = new ComparableCapability();
        ac.setDisplayName("buffer");
        ac.setID("Capability.Buffer");
        ac.setUri(BUFFER_CAPABILITY_URI);
        return ac;
    }
}
