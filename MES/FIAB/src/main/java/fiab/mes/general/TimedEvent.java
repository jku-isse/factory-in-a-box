package fiab.mes.general;

import java.time.ZonedDateTime;

public abstract class TimedEvent {
	protected ZonedDateTime timestamp;
	protected String machineId;

	public ZonedDateTime getTimestamp() {
		return timestamp;
	}

	public String getMachineId() {
		return machineId;
	}
}

