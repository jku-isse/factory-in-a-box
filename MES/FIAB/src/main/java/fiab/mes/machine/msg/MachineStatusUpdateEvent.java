package fiab.mes.machine.msg;

import fiab.core.capabilities.BasicMachineStates;

public class MachineStatusUpdateEvent extends MachineUpdateEvent {
	
	protected BasicMachineStates status;
	
	public MachineStatusUpdateEvent(String machineId, String nodeId, String parameterName, String message, BasicMachineStates status) {
		super(machineId, nodeId, parameterName, message);
		this.status = status;
	}
	
	public BasicMachineStates  getStatus() {
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
