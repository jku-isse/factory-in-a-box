package shopfloor.agents.messages.order;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class OrderLocationResponse {
	protected String orderId;
	protected String lastLocatedAtCapability;
	protected List<String> capabilityHierarchy = new ArrayList<String>();
	protected Instant knownAsOfTimestamp;
	protected int errorCode;
	
	@Deprecated
	public OrderLocationResponse() {}	
	
	public OrderLocationResponse(String orderId, Instant lastLocatedAtTime, int errorCode) {
		super();
		this.orderId = orderId;
		this.errorCode = errorCode;
		this.knownAsOfTimestamp = lastLocatedAtTime;
	}
	
	public OrderLocationResponse(String orderId, String lastLocatedAtCapability, List<String> capabilityHierarchy,
			Instant lastLocatedAtTime) {
		super();
		this.orderId = orderId;
		this.lastLocatedAtCapability = lastLocatedAtCapability;
		this.capabilityHierarchy = capabilityHierarchy;
		this.knownAsOfTimestamp = lastLocatedAtTime;
	}
	
	public String getOrderId() {
		return orderId;
	}
	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}
	public String getLastLocatedAtCapability() {
		return lastLocatedAtCapability;
	}
	public void setLastLocatedAtCapability(String lastLocatedAtCapability) {
		this.lastLocatedAtCapability = lastLocatedAtCapability;
	}
	public List<String> getCapabilityHierarchy() {
		return capabilityHierarchy;
	}
	public void setCapabilityHierarchy(List<String> capabilityHierarchy) {
		this.capabilityHierarchy = capabilityHierarchy;
	}
	public Instant getKnownAsOfTimestamp() {
		return knownAsOfTimestamp;
	}
	public void setKnownAsOfTimestamp(Instant knownAsOfTimestamp) {
		this.knownAsOfTimestamp = knownAsOfTimestamp;
	}
	public int getErrorCode() {
		return errorCode;
	}
	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}
	
}
