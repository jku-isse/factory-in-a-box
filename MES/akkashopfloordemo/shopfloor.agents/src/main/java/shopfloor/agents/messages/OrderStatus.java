package shopfloor.agents.messages;

import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OrderStatus {
	String orderId;
	@JsonProperty("jobStatus")
	HashMap<String,JobStatus> job2status = new HashMap<>();
	
	public OrderStatus(OrderDocument doc) {
		this.orderId = doc.getId();
		doc.getJobs().stream().forEach(job -> job2status.put(job, JobStatus.IDLE));
	}
	
	public void setStatus(String jobId, JobStatus status) {
		job2status.replace(jobId, status);
	}
	
	
	public String getOrderId() {
		return orderId;
	}


	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}


	public HashMap<String, JobStatus> getJob2status() {
		return job2status;
	}

	public void setJob2status(HashMap<String, JobStatus> job2status) {
		this.job2status = job2status;
	}

	public static enum JobStatus { IDLE, INPROGRESS, CANCELED, HALTED, COMPLETED } 				
}
