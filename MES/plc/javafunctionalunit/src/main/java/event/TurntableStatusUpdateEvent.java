package event;

import stateMachines.turning.TurningStates;

public class TurntableStatusUpdateEvent extends MachineUpdateEvent {
	
	protected TurningStates status;
	
	public TurntableStatusUpdateEvent(String machineId, String nodeId, String parameterName, String message, TurningStates status) {
		super(machineId, nodeId, parameterName, message);
		this.status = status;
	}
	
	public TurningStates getStatus() {
		return status;
	}

	@Override
	public Object getValue() {
		return status;
	}

	@Override
	public String toString() {
		return "TurningStatesUpdateEvent [status=" + status + ", machineId=" + machineId + "]";
	}
	

}
