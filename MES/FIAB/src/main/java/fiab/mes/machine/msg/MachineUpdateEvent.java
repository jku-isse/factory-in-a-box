package fiab.mes.machine.msg;

public class MachineUpdateEvent extends MachineEvent {
	
	String nodeId;
	String parameterName;
	Object newValue;
	
	
	/**
	 * This class is published on the MachineLevelEventBus and is filled with the values from the OPCUA-Server
	 * @param machineId
	 * @param nodeId
	 * @param parameterName
	 * @param value
	 */
	public MachineUpdateEvent(String machineId, String nodeId, String parameterName, Object value, String message) {
		super(machineId, MachineEventType.UPDATED, message);
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
	
	

}