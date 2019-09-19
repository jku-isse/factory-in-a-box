package fiab.mes.transport.msg;

public class MachineConnectedEvent extends MachineEvent {

	public MachineConnectedEvent(String machineId) {
		super(machineId, new String("CONNECTED"));
	}

}
