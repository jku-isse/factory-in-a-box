package shopfloor.agents.messages;

public class NotifyAvailableForOrder {
	protected String jobId;

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public NotifyAvailableForOrder(String jobId) {
		super();
		this.jobId = jobId;
	}	
	
	
}
