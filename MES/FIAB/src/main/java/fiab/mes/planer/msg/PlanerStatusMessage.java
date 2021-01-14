package fiab.mes.planer.msg;

import main.java.fiab.core.capabilities.basicmachine.events.MachineEvent;

public class PlanerStatusMessage extends MachineEvent {
	
	protected PlannerState state;
	protected String message;
	
	public PlanerStatusMessage(String machineId, MachineEventType eventType, PlannerState state, String message) {
		super(machineId, eventType);
		this.state = state;
		this.message = message;
	}

	public PlannerState getState() {
		return state;
	}
	
	public String getMessage() {
		return message;
	}

	@Override
	public String toString() {
		return "PlanerStatusMessage [state=" + state + ", machineId=" + machineId + ", eventType=" + eventType + "]";
	}

	public static enum PlannerState {
		STARTING, FAILED_START, DEGRADED_MODE, FULLY_OPERATIONAL, SHUTTING_DOWN, STOPPED
	}
}
