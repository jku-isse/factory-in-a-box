package shopfloor.agents.messages;

public class LockForOrder {
	protected String jobId;	
	
	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public LockForOrder(String jobId) {
		super();
		this.jobId = jobId;
	}	
	
	
}
