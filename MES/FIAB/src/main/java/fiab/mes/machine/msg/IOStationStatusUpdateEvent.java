package fiab.mes.machine.msg;

import fiab.core.capabilities.basicmachine.events.MachineUpdateEvent;
import fiab.core.capabilities.handshake.HandshakeCapability.ServerSideStates;
import fiab.core.capabilities.handshake.IOStationCapability;

public class IOStationStatusUpdateEvent extends MachineUpdateEvent {
	
	protected ServerSideStates status;
	
	public IOStationStatusUpdateEvent(String machineId, String message, ServerSideStates status) {
		super(machineId, IOStationCapability.OPCUA_STATE_SERVERSIDE_VAR_NAME,  message);
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
		return "IOStationStatusUpdateEvent [status=" + status + ", machineId=" + machineId + "]";
	}
	
	
}
