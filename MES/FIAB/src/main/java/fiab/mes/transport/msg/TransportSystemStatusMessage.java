package fiab.mes.transport.msg;

import main.java.fiab.core.capabilities.basicmachine.events.MachineEvent;

public class TransportSystemStatusMessage extends MachineEvent {
	
	protected State state;
	protected String message;
	
	public TransportSystemStatusMessage(String machineId, MachineEventType eventType, State state, String message) {
		super(machineId, eventType);
		this.state = state;
		this.message = message;
	}

	public State getState() {
		return state;
	}
	
	public String getMessage() {
		return message;
	}

	@Override
	public String toString() {
		return "TransportSystemStatusMessage [state=" + state + ", machineId=" + machineId + ", eventType=" + eventType + "]";
	}

	public static enum State {
		STARTING, FAILED_START, DEGRADED_MODE, FULLY_OPERATIONAL, SHUTTING_DOWN, STOPPED
	}
}
