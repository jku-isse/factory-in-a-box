package fiab.mes.machine.msg;

import fiab.core.capabilities.basicmachine.events.MachineEvent;

public class MachineErrorResponse extends MachineEvent {

	public MachineErrorResponse(String machineId, MachineEventType eventType, String message) {
		super(machineId, eventType, message);
	}

}
