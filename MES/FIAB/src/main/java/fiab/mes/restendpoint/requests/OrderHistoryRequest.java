package fiab.mes.restendpoint.requests;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import fiab.mes.order.msg.OrderEvent;

public class OrderHistoryRequest {
	private String orderId;
	private boolean includeDetails = false;

	public OrderHistoryRequest(String orderId) {
		this.orderId = orderId;
	}
	
	public OrderHistoryRequest(String orderId, boolean includeDetails) {
		this.orderId = orderId;
		this.includeDetails = includeDetails;
	}
	
	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}
	
	public boolean shouldResponseIncludeDetails() {
		return includeDetails;
	}
	
	
	public static class Response{
		private List<OrderEvent> updates;
		private String orderId;
		boolean includesDetails = false;
		
		@JsonCreator
		public Response(@JsonProperty("orderId") String orderId,  @JsonProperty("updates") List<OrderEvent> updates, @JsonProperty("includesDetails") boolean includesDetails) {
			this.updates = updates;
			this.orderId = orderId;
			this.includesDetails = includesDetails;
		}

		public List<OrderEvent> getUpdates() {
			return updates;
		}

		public String getOrderId() {
			return orderId;
		}

		public boolean doesIncludeDetails() {
			return includesDetails;
		}
		
	}
}