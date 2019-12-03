package fiab.mes.machine.msg;

import fiab.mes.transport.handshake.HandshakeProtocol;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerSide;

public class IOStationStatusUpdateEvent extends MachineUpdateEvent {
	
	protected ServerSide status;
	
	public IOStationStatusUpdateEvent(String machineId, String message, ServerSide status) {
		super(machineId, "",  HandshakeProtocol.STATE_SERVERSIDE_VAR_NAME, message);
		this.status = status;
	}
	
	ServerSide getStatus() {
		return status;
	}

	@Override
	public Object getValue() {
		return status;
	}
	
	
}
