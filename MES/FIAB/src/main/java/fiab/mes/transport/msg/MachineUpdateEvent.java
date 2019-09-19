package fiab.mes.transport.msg;

public class MachineUpdateEvent extends MachineEvent {
	
	String nodeId;
	
	public MachineUpdateEvent(String machineId, Object message, String nodeId) {
		super(machineId, message);
		this.nodeId = nodeId;
	}
	
	public String getNodeId() {
		return nodeId;
	}

}
