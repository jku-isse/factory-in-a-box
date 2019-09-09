package shopfloor.agents.events;

import shopfloor.agents.messages.OrderStatus;

public class OrderEvent {

	protected String orderId;
	protected OrderEventType type;
	protected OrderStatus status;
	
	@Deprecated
	public OrderEvent() {}
	
	public OrderEvent(String orderId, OrderEventType type, OrderStatus status) {
		super();
		this.orderId = orderId;
		this.type = type;
		this.status = status;
	}

	public String getOrderId() {
		return orderId;
	}



	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}



	public OrderEventType getType() {
		return type;
	}



	public void setType(OrderEventType type) {
		this.type = type;
	}



	public OrderStatus getStatus() {
		return status;
	}



	public void setStatus(OrderStatus status) {
		this.status = status;
	}



	public static enum OrderEventType {
		CREATED, PRODUCTION_UPDATE, TRANSPORT_UPDATE, CANCELED, HALTED, CONTINUED, COMPLETED, DELETED
	}
}
