package fiab.core.capabilities.buffer;

import ProcessCore.AbstractCapability;
import fiab.core.capabilities.ComparableCapability;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;

public interface BufferStationCapability extends OPCUABasicMachineBrowsenames {

    public enum BufferStationStates{
        UNKNOWN, STOPPING, STOPPED, RESETTING, IDLE_LOADED, IDLE_EMPTY, STARTING, EXECUTE, COMPLETING, COMPLETE
    }

    public static String OPCUA_LOAD_REQUEST = "LOAD_BUFFER";
    public static String OPCUA_UNLOAD_REQUEST = "UNLOAD_BUFFER";

    public static String BUFFER_CAPABILITY_URI = "http://factory-in-a-box.fiab/capabilities/buffer_fu";

    public static AbstractCapability getBufferCapability() {
        ComparableCapability ac = new ComparableCapability();
        ac.setDisplayName("buffer");
        ac.setID("Capability.Buffer");
        ac.setUri(BUFFER_CAPABILITY_URI);
        return ac;
    }
}
