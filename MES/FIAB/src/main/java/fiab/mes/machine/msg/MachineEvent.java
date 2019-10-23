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
	
	public MachineEventType getEventType() {
		return eventType;
	}
	
	
	public String getMachineId() {
		return machineId;
	}
	
	public static enum MachineEventType {
<<<<<<< HEAD
		CONNECTED, DISCONNECTED, UPDATE
	}

}
=======
		CONNECTED, DISCONNECTED, UPDATED
	}

}
>>>>>>> parent of c7005b3... finished first test of order processing by 4 machines via the orderplanningactor
