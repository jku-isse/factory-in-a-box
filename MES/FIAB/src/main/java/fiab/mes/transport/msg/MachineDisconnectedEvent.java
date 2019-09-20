package fiab.mes.transport.msg;

public class MachineDisconnectedEvent extends MachineEvent {

	public MachineDisconnectedEvent(String machineId) {
		super(machineId, new String("DISCONNECTED"));
	}

}
