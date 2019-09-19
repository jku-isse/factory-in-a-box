package fiab.mes.transport.msg;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import fiab.mes.general.TimedEvent;

public class MachineEvent extends TimedEvent {
	private Object message;
	
	public MachineEvent(String machineId, Object message) {
		timestamp = ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
		this.machineId = machineId;
		this.message = message;
	}
	
	public Object getMessage() {
		return message;
	}
	
	public String getMachineId() {
		return machineId;
	}

}
