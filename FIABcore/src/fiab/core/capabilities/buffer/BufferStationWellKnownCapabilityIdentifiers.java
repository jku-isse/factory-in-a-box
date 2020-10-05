package fiab.core.capabilities.buffer;

public interface BufferStationWellKnownCapabilityIdentifiers extends BufferStationCapability {

    enum SimpleMessageTypes{
        SubscribeState, Reset, Stop
    }

    public final String BUFFER_STATION_SELF = "BUFFER_STATION_SELF";
}
