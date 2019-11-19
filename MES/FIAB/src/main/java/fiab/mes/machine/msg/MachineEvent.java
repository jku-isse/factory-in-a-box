package fiab.mes.machine.msg;

import java.time.ZonedDateTime;

import fiab.mes.general.TimedEvent;

public class MachineEvent extends TimedEvent {
	
	private String message;
	protected String machineId;
	protected MachineEventType eventType;
	
	public MachineEvent(String machineId, MachineEventType eventType, String message) {
		super();
		this.machineId = machineId;
		this.eventType = eventType;
		this.message = message;
	}
	
	public MachineEvent(String machineId, MachineEventType eventType, String message, ZonedDateTime timestamp) {
		super(timestamp);
		this.machineId = machineId;
		this.eventType = eventType;
		this.message = message;
	}
	
	public MachineEventType getEventType() {
		return eventType;
	}
	
	
	public String getMachineId() {
		return machineId;
	}
	
	public String getMessage() {
		return message;
	}
	
	public MachineEvent getCloneWithoutDetails() {
		return new MachineEvent(machineId, eventType, message, getTimestamp());
	}
	
	public static enum MachineEventType {
		CONNECTED, DISCONNECTED, UPDATED
	}

}