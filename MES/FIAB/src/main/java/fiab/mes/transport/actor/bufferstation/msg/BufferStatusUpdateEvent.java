package fiab.mes.transport.actor.bufferstation.msg;

import fiab.core.capabilities.basicmachine.events.MachineUpdateEvent;
import fiab.core.capabilities.buffer.BufferStationCapability;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.core.capabilities.handshake.IOStationCapability;

public class BufferStatusUpdateEvent extends MachineUpdateEvent {

    protected BufferStationCapability.BufferStationStates status;

    public BufferStatusUpdateEvent (String machineId, String message, BufferStationCapability.BufferStationStates status) {
        super(machineId, BufferStationCapability.STATE_VAR_NAME,  message);
        this.status = status;
    }

    public BufferStationCapability.BufferStationStates getStatus() {
        return status;
    }

    @Override
    public Object getValue() {
        return status;
    }

    @Override
    public String toString() {
        return "BufferStationStatusUpdateEvent [status=" + status + ", machineId=" + machineId + "]";
    }
}
