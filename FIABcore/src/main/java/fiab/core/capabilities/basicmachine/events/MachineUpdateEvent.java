package main.java.fiab.core.capabilities.basicmachine.events;

public abstract class MachineUpdateEvent extends MachineEvent {
	
	String parameterName;
	
	/**
	 * This class is published on the MachineLevelEventBus and is filled with the values from the OPCUA-Server
	 * @param machineId
	 * @param parameterName
	 * @param value
	 */
	public MachineUpdateEvent(String machineId, String parameterName, String message) {
		super(machineId, MachineEventType.UPDATED, message);
		this.parameterName = parameterName;
	}

	public String getParameterName() {
		return parameterName;
	}
	
	abstract public Object getValue();
}