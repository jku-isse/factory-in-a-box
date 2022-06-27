package fiab.handshake.server.messages;

import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.basicmachine.events.MachineUpdateEvent;
import fiab.core.capabilities.handshake.ServerSideStates;

public class ServerHandshakeStatusUpdateEvent extends MachineUpdateEvent {
    protected final ServerSideStates status;

    public ServerHandshakeStatusUpdateEvent(String machineId, ServerSideStates status) {
        super(machineId, OPCUABasicMachineBrowsenames.STATE_VAR_NAME, "Conveyor State has been updated");
        this.status = status;
    }

    public ServerSideStates getStatus() {
        return status;
    }

    @Override
    public Object getValue() {
        return status;
    }

    @Override
    public String toString() {
        return "ServerHandshakeStatusUpdateEvent [status=" + status + ", machineId=" + machineId + "]";
    }
}
