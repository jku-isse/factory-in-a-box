package fiab.mes.machine.msg;

import fiab.mes.machine.msg.MachineEvent.MachineEventType;

public class MachineEventWrapper {

	private String message;
	protected String machineId;
	protected MachineEventType eventType;
	private String timestamp;
	
	public MachineEventWrapper(MachineEvent e) {
		this.message = e.getMessage();
		this.machineId = e.getMachineId();
		this.eventType = e.getEventType();
		this.timestamp = e.getTimestamp().toString();
	}
	
	public String getMessage() {
		return message;
	}
	public String getMachineId() {
		return machineId;
	}
	public MachineEventType getEventType() {
		return eventType;
	}
	public String getTimestamp() {
		return timestamp;
	}
	
	
}
