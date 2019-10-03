package fiab.mes.general;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public abstract class TimedEvent {
	protected ZonedDateTime timestamp;
	
	public TimedEvent() {
		timestamp = ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
	}

	public ZonedDateTime getTimestamp() {
		return timestamp;
	}

	public TimedEvent(ZonedDateTime timestamp) {
		this.timestamp = timestamp;
	}
	
}

