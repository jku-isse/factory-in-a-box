package fiab.mes.machine.msg;

public class MachineStatusUpdateEvent extends MachineUpdateEvent {
	
	protected MachineStatus status;
	
	public MachineStatusUpdateEvent(String machineId, String nodeId, String parameterName, String message, MachineStatus status) {
		super(machineId, nodeId, parameterName, message);
		this.status = status;
	}
	
	public MachineStatus  getStatus() {
		return status;
	}

	@Override
	public Object getValue() {
		return status;
	}

	@Override
	public String toString() {
		return "MachineStatusUpdateEvent [status=" + status + ", machineId=" + machineId + "]";
	}
	

}
