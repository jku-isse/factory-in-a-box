package fiab.mes.machine.msg;

import fiab.mes.machine.msg.MachineEvent.MachineEventType;

public class MachineEventWrapper {

	private String message;
	private String machineId;
	private MachineEventType eventType;
	private String timestamp;
	
	//MachineUpdateEvent additional fields
	private String nodeId;
	private String parameterName;
	private String newValue;
	
	public MachineEventWrapper(MachineEvent e) {
		this.message = e.getMessage();
		this.machineId = e.getMachineId();
		this.eventType = e.getEventType();
		this.timestamp = e.getTimestamp().toString();
		if (e instanceof MachineUpdateEvent) {
			MachineUpdateEvent mue = (MachineUpdateEvent) e;
			this.nodeId = mue.getNodeId();
			this.parameterName = mue.getParameterName();
			this.newValue = mue.getNewValue().toString();
		}
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

	public String getNodeId() {
		return nodeId;
	}

	public String getParameterName() {
		return parameterName;
	}

	public String getNewValue() {
		return newValue;
	}
	
	
}
