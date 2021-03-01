package fiab.core.capabilities.events;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import fiab.tracing.actor.messages.TracingHeader;

public abstract class TimedEvent implements TracingHeader {
	protected ZonedDateTime timestamp;
	private String header = "";

	public TimedEvent() {
		timestamp = ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
	}

	public TimedEvent(ZonedDateTime timestamp) {
		this.timestamp = timestamp;
	}

	public TimedEvent(String header) {
		this();
		this.header = header;

	}

	public TimedEvent(ZonedDateTime timestamp, String header) {
		this.timestamp = timestamp;
		this.header = header;
	}

	public ZonedDateTime getTimestamp() {
		return timestamp;
	}

	@Override
	public String getHeader() {
		return header;
	}

	@Override
	public void setHeader(String header) {
		this.header = header;
	}

}
