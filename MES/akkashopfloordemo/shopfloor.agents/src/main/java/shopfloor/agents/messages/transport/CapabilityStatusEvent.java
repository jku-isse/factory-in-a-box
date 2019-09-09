package shopfloor.agents.messages.transport;

import java.time.Instant;

public class CapabilityStatusEvent {
	protected String status;
	protected Instant timestamp;
	protected String capabilityId;
	
	@Deprecated
	public CapabilityStatusEvent() {}
	
	public CapabilityStatusEvent(String status, Instant timestamp, String capabilityId) {
		super();
		this.status = status;
		this.timestamp = timestamp;
		this.capabilityId = capabilityId;
	}
	
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public Instant getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}
	public String getCapabilityId() {
		return capabilityId;
	}
	public void setCapabilityId(String capabilityId) {
		this.capabilityId = capabilityId;
	}
}
