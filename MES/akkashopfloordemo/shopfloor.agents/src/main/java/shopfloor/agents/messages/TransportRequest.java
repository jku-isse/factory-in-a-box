package shopfloor.agents.messages;

import akka.actor.ActorRef;

public class TransportRequest {
	
	protected String jobId;
	protected ActorRef fromMachine;
	protected ActorRef toMachine;
	
	public TransportRequest(String jobId, ActorRef fromMachine, ActorRef toMachine) {
		super();
		this.jobId = jobId;
		this.fromMachine = fromMachine;
		this.toMachine = toMachine;
	}

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public ActorRef getFromMachine() {
		return fromMachine;
	}

	public void setFromMachine(ActorRef fromMachine) {
		this.fromMachine = fromMachine;
	}

	public ActorRef getToMachine() {
		return toMachine;
	}

	public void setToMachine(ActorRef toMachine) {
		this.toMachine = toMachine;
	}
	
	
}
