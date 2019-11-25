package fiab.mes.machine.msg;

public class MachineStatusUpdateEvent extends MachineUpdateEvent {
	
	String status;
	
	public MachineStatusUpdateEvent(String machineId, String nodeId, String parameterName, String message, MachineStatus status) {
		super(machineId, nodeId, parameterName, message);
		this.status = status.toString();
	}
	
	public MachineStatusUpdateEvent(String machineId, String nodeId, String parameterName, String message, String status) {
		super(machineId, nodeId, parameterName, message);
		this.status = status;
	}
	
	public String getStatus() {
		return status;
	}
	

}
