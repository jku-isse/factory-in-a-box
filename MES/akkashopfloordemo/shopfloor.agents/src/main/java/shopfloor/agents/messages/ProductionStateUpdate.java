package shopfloor.agents.messages;

import java.time.Instant;

public class ProductionStateUpdate {
	protected ProductionState stateReached;
	protected String jobId;
	protected Instant timestamp;	
	
	public static enum ProductionState {
		//eventually all PACKML states here
		STARTED, COMPLETED
	}

	public ProductionState getStateReached() {
		return stateReached;
	}

	public void setStateReached(ProductionState stateReached) {
		this.stateReached = stateReached;
	}

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}

	public ProductionStateUpdate(ProductionState stateReached, String jobId, Instant timestamp) {
		super();
		this.stateReached = stateReached;
		this.jobId = jobId;
		this.timestamp = timestamp;
	}

	@Override
	public String toString() {
		return "ProductionStateUpdate [stateReached=" + stateReached + ", jobId=" + jobId + ", timestamp=" + timestamp
				+ "]";
	}

	
	
}


