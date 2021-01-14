package main.java.fiab.core.capabilities.basicmachine.events;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import main.java.fiab.core.capabilities.BasicMachineStates;

public class MachineInWrongStateResponse extends MachineStatusUpdateEvent {

	Set<BasicMachineStates> states = new HashSet<>();
	Object request;
	
	public MachineInWrongStateResponse(String machineId, String parameterName, String message,
			BasicMachineStates status, Object request, BasicMachineStates... prerequisiteStates) {
		super(machineId, parameterName, message, status);
		states.addAll(Arrays.asList(prerequisiteStates));
		this.request = request;
	}
	
	public Set<BasicMachineStates> getPrerequisitStatesForRequest() {
		return states;
	}
	
	public Object getCausingRequest() {
		return request;
	}
}
