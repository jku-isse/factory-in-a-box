package fiab.conveyor.messages;

import fiab.conveyor.statemachine.ConveyorStates;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.basicmachine.events.MachineUpdateEvent;

public class ConveyorStatusUpdateEvent extends MachineUpdateEvent {
	
	protected final ConveyorStates status;
	
	public ConveyorStatusUpdateEvent(String machineId, ConveyorStates status) {
		super(machineId, OPCUABasicMachineBrowsenames.STATE_VAR_NAME, "Conveyor State has been updated");
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
