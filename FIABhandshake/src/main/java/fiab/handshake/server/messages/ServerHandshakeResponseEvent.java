package fiab.handshake.server.messages;

import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.basicmachine.events.MachineUpdateEvent;
import fiab.core.capabilities.handshake.HandshakeCapability;

public class ServerHandshakeResponseEvent extends MachineUpdateEvent {

    protected final HandshakeCapability.ServerMessageTypes response;

    public ServerHandshakeResponseEvent(String machineId, HandshakeCapability.ServerMessageTypes response) {
        super(machineId, response.name(), "Server response to handshake trigger");
        this.response = response;
    }

    public HandshakeCapability.ServerMessageTypes getResponse() {
        return response;
    }

    @Override
    public Object getValue() {
        return response;
    }

    @Override
    public String toString() {
        return "ServerHandshakeResponseEvent [status=" + response + ", machineId=" + machineId + "]";
    }
}
