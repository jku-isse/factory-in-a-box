package shopfloor.agents.events;

import shopfloor.agents.messages.OrderStatus;

public class OrderStatusAllJobsUpdateEvent extends OrderBaseEvent {

	protected OrderStatus status;
	
	@Deprecated
	public OrderStatusAllJobsUpdateEvent() {}
	
	public OrderStatusAllJobsUpdateEvent(String orderId, String eventSource, OrderEventType type, OrderStatus status) {
		super(orderId, eventSource, type);		
		this.status = status;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public void setStatus(OrderStatus status) {
		this.status = status;
	}
}
