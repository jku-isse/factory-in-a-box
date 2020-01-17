package fiab.mes.machine.msg;

public class MachineErrorResponse extends MachineEvent {

	public MachineErrorResponse(String machineId, MachineEventType eventType, String message) {
		super(machineId, eventType, message);
	}

}
