package msg;

import event.MachineStatusUpdateEvent;
import stateMachines.MachineStatus;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MachineInWrongStateResponse extends MachineStatusUpdateEvent {

	Set<MachineStatus> states = new HashSet<>();
	Object request;
	
	public MachineInWrongStateResponse(String machineId, String parameterName, String message,
			MachineStatus status, Object request, MachineStatus... prerequisiteStates) {
		super(machineId, null, parameterName, message, status);
		states.addAll(Arrays.asList(prerequisiteStates));
		this.request = request;
	}
	
	public Set<MachineStatus> getPrerequisitStatesForRequest() {
		return states;
	}
	
	public Object getCausingRequest() {
		return request;
	}
}
