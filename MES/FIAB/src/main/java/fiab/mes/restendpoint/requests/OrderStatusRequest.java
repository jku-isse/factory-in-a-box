package fiab.mes.restendpoint.requests;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import fiab.mes.order.OrderProcess;

public class OrderStatusRequest{
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
	
	public static class Response{
		private OrderProcess status;
		
		@JsonCreator
		public Response(@JsonProperty("status") OrderProcess status) {
			this.status = status;
		}

		public OrderProcess getStatus() {
			return status;
		}

		public void setStatus(OrderProcess status) {
			this.status = status;
		}
		
		
	}
	
}


