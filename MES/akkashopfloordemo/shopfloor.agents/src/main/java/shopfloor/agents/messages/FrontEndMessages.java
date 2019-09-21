package shopfloor.agents.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FrontEndMessages {

	public static class OrderStatusRequest{
		private String orderId;

		public OrderStatusRequest(String orderId) {
			this.orderId = orderId;
		}
		
		public String getOrderId() {
			return orderId;
		}

		public void setOrderId(String orderId) {
			this.orderId = orderId;
		}
		
	}
	
	public static class OrderStatusResponse{
		private OrderStatus status;
		
		@JsonCreator
		public OrderStatusResponse(@JsonProperty("status") OrderStatus status) {
			this.status = status;
		}

		public OrderStatus getStatus() {
			return status;
		}

		public void setStatus(OrderStatus status) {
			this.status = status;
		}
		
		
	}
	
	
	
}
