package fiab.turntable.conveying;

import fiab.core.capabilities.basicmachine.events.MachineUpdateEvent;
import fiab.turntable.conveying.statemachine.ConveyorStates;

public class ConveyorStatusUpdateEvent extends MachineUpdateEvent {
	
	protected ConveyorStates status;
	
	public ConveyorStatusUpdateEvent(String machineId, String parameterName, String message, ConveyorStates status) {
		super(machineId, parameterName, message);
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
