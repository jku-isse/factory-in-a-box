package shopfloor.agents.messages;

import akka.actor.ActorRef;

public class RegisterOrderRequest {
	protected String jobId;
	protected Object jobDescription;
	protected ActorRef orderAgent;
	
	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}
	
	public ActorRef getOrderAgent() {
		return this.orderAgent;
	}

	public RegisterOrderRequest(String jobId, Object jobDescription, ActorRef orderAgent) {
		super();
		this.jobId = jobId;
		this.jobDescription = jobDescription;
		this.orderAgent = orderAgent;
	}	
	
}
