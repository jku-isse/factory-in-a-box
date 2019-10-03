package fiab.mes.machine.msg;

import fiab.mes.general.TimedEvent;

public class MachineEvent extends TimedEvent {
	
	protected String machineId;
	protected MachineEventType eventType;
	
	public MachineEvent(String machineId, MachineEventType eventType) {
		super();
		this.machineId = machineId;
		this.eventType = eventType;
	}
	
	
	public String getMachineId() {
		return machineId;
	}
	
	public static enum MachineEventType {
		CONNECTED, DISCONNECTED, UPDATED
	}

}
