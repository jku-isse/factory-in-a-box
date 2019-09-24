package shopfloor.agents.events;

public class OrderBaseEvent {

	protected String orderId;
	protected String eventSource;
	protected OrderEventType type;
		
	@Deprecated
	public OrderBaseEvent() {}
	
	public OrderBaseEvent(String orderId, String eventSource, OrderEventType type) {
		super();
		this.orderId = orderId;
		this.type = type;		
		this.eventSource = eventSource;
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

	
	public String getEventSource() {
		return eventSource;
	}

	public void setEventSource(String eventSource) {
		this.eventSource = eventSource;
	}


	public static enum OrderEventType {
		CREATED, REGISTERED, SCHEDULED, ALLOCATED, PRODUCTION_UPDATE, TRANSPORT_UPDATE, CANCELED, PAUSED, CONTINUED, COMPLETED, DELETED
	}
}
