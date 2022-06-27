package fiab.handshake.client.messages;

import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.basicmachine.events.MachineUpdateEvent;
import fiab.core.capabilities.handshake.ClientSideStates;
import fiab.core.capabilities.handshake.ServerSideStates;

public class ClientHandshakeStatusUpdateEvent extends MachineUpdateEvent {
    protected final ClientSideStates status;

    public ClientHandshakeStatusUpdateEvent(String machineId, ClientSideStates status) {
        super(machineId, OPCUABasicMachineBrowsenames.STATE_VAR_NAME, "Conveyor State has been updated");
        this.status = status;
    }

    public ClientSideStates getStatus() {
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
