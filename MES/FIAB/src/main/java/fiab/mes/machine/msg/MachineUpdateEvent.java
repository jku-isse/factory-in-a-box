package fiab.mes.machine.msg;

public class MachineUpdateEvent extends MachineEvent {
	
	String nodeId;
	MachineEvent.MachineEventType type;
	Object newValue;
	
	
	/**
	 * This class is published on the MachineLevelEventBus and is filled with the values from the OPCUA-Server
	 * @param machineId
	 * @param nodeId
	 * @param parameterName
	 * @param value
	 */
	public MachineUpdateEvent(String machineId, String nodeId, MachineEvent.MachineEventType type, Object value) {
		super(machineId, MachineEventType.UPDATE);
		this.nodeId = nodeId;
		this.newValue = value;
		this.type = type;
	}
	
	public String getNodeId() {
		return nodeId;
	}

	public MachineEvent.MachineEventType getType() {
		return type;
	}

	public Object getNewValue() {
		return newValue;
	}
	
	

}