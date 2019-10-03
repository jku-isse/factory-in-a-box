package fiab.mes.machine.msg;

public class MachineDisconnectedEvent extends MachineEvent {

	
	public MachineDisconnectedEvent(String machineId) {
		super(machineId, MachineEventType.DISCONNECTED);
	}
	
}
