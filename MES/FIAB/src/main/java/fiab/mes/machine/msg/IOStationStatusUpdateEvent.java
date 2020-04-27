package fiab.mes.machine.msg;

import fiab.core.capabilities.handshake.HandshakeCapability.ServerSide;
import fiab.core.capabilities.handshake.IOStationCapability;

public class IOStationStatusUpdateEvent extends MachineUpdateEvent {
	
	protected ServerSide status;
	
	public IOStationStatusUpdateEvent(String machineId, String message, ServerSide status) {
		super(machineId, "",  IOStationCapability.STATE_SERVERSIDE_VAR_NAME, message);
		this.status = status;
	}
	
	public ServerSide getStatus() {
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
