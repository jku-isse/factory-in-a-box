package shopfloor.agents.messages;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OrderDocument {
	ArrayList<String> jobs = new ArrayList<>();
	String id;
	
	@JsonCreator
	public OrderDocument(@JsonProperty("id") String orderId, @JsonProperty("jobs") ArrayList<String> jobs) {
		this.jobs = jobs;
		this.id = orderId;
	}

	public ArrayList<String> getJobs() {
		return jobs;
	}

	public void setJobs(ArrayList<String> jobs) {
		this.jobs = jobs;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	
}


