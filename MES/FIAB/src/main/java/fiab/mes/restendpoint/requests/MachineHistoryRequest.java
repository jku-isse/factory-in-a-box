package fiab.mes.restendpoint.requests;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import fiab.core.capabilities.basicmachine.events.MachineEvent;

public class MachineHistoryRequest {
	
	private String machineId;
	private boolean includeDetails = false;
	
	public MachineHistoryRequest(String machineId) {
		this.machineId = machineId;
	}
	
	public MachineHistoryRequest(String machineId, boolean includeDetails) {
		this(machineId);
		this.includeDetails = includeDetails;
	}

	public String getMachineId() {
		return machineId;
	}

	public void setMachineId(String machineId) {
		this.machineId = machineId;
	}
	
	public boolean shouldResponseIncludeDetails() {
		return includeDetails;
	}
	
	public static class Response {
		private List<MachineEvent> updates;
		private String machineId;
		private boolean includesDetails = false;
		
		@JsonCreator
		public Response(@JsonProperty("machineId") String machineId,  @JsonProperty("updates") List<MachineEvent> updates, @JsonProperty("includesDetails") boolean includesDetails) {
			this.updates = updates;
			this.machineId = machineId;
			this.includesDetails = includesDetails;
		}

		public List<MachineEvent> getUpdates() {
			return updates;
		}

		public String getMachineId() {
			return machineId;
		}
		
		public boolean doesIncludeDetails() {
			return includesDetails;
		}
	}
	
}
