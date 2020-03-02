package stateMachines.conveyor;

import event.MachineUpdateEvent;

public class ConveyorStatusUpdateEvent extends MachineUpdateEvent {
	
	protected ConveyorStates status;
	
	public ConveyorStatusUpdateEvent(String machineId, String nodeId, String parameterName, String message, ConveyorStates status) {
		super(machineId, nodeId, parameterName, message);
		this.status = status;
	}
	
	public ConveyorStates getStatus() {
		return status;
	}

	@Override
	public Object getValue() {
		return status;
	}

	@Override
	public String toString() {
		return "ConveyorStatesUpdateEvent [status=" + status + ", machineId=" + machineId + "]";
	}
	

}
