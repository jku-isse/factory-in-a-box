package fiab.turntable.turning;

import fiab.core.capabilities.basicmachine.events.MachineUpdateEvent;
import fiab.turntable.turning.statemachine.TurningStates;

public class TurntableStatusUpdateEvent extends MachineUpdateEvent {
	
	protected TurningStates status;
	
	public TurntableStatusUpdateEvent(String machineId, String parameterName, String message, TurningStates status) {
		super(machineId, parameterName, message);
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
