package fiab.turntable.turning.messages;

import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.basicmachine.events.MachineUpdateEvent;
import fiab.turntable.turning.statemachine.TurningStates;

public class TurningStatusUpdateEvent extends MachineUpdateEvent {
	
	protected final TurningStates status;
	
	public TurningStatusUpdateEvent(String machineId, TurningStates status) {
		super(machineId, OPCUABasicMachineBrowsenames.STATE_VAR_NAME, "Turning State has been updated");
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
		return "TurningStatusUpdateEvent [status=" + status + ", machineId=" + machineId + "]";
	}
	

}
