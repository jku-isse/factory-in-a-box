package fiab.mes.machine.msg;

public class MachineUpdateEvent extends MachineEvent {
	
	String nodeId;
	String parameterName;
	Object newValue;
	
	public MachineUpdateEvent(String machineId, String nodeId, String parameterName, Object value) {
		super(machineId, MachineEventType.UPDATED);
		this.nodeId = nodeId;
		this.newValue = value;
		this.parameterName = parameterName;
	}
	
	public String getNodeId() {
		return nodeId;
	}

	public String getParameterName() {
		return parameterName;
	}

	public Object getNewValue() {
		return newValue;
	}

	@Override
	public String toString() {
		return "MachineUpdateEvent [parameterName=" + parameterName + ", newValue=" + newValue
				+ ", machineId=" + machineId + ", eventType=" + eventType + ", timestamp=" + timestamp + "]";
	}
	
	

}
