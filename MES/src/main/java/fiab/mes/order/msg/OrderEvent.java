package fiab.mes.order.msg;

import java.time.ZonedDateTime;

import fiab.core.capabilities.events.TimedEvent;

public class OrderEvent extends TimedEvent {
	
	private OrderEventType eventType;
	private String orderId;
	private String machineId;
	private String message;
	
	public OrderEvent(String orderId, String machineId, OrderEventType eventType, String message) {
		super();
		this.orderId = orderId;
		this.eventType = eventType;
		this.machineId = machineId;
		this.message = message;
	}
	
	public OrderEvent(String orderId, String machineId, OrderEventType eventType, String message,  ZonedDateTime timestamp) {
		super(timestamp);
		this.orderId = orderId;
		this.eventType = eventType;
		this.machineId = machineId;
		this.message = message;
	}

	public OrderEventType getEventType() {
		return eventType;
	}

	public String getMachineId() {
		return machineId;
	}

	
	public String getOrderId() {
		return orderId;
	}

	public String getMessage() {
		return message;
	}
	
	public static enum OrderEventType {
		CREATED, REGISTERED, SCHEDULED, ALLOCATED, PRODUCTION_UPDATE, PRODUCING,
		TRANSPORT_REQUESTED, TRANSPORT_IN_PROGRESS, TRANSPORT_COMPLETED, TRANSPORT_DENIED, TRANSPORT_FAILED,
		REJECTED, CANCELED, PAUSED, COMPLETED, REMOVED, PREMATURE_REMOVAL
	}

	
	public OrderEvent getCloneWithoutDetails() {
		return new OrderEvent(this.orderId, this.machineId, this.eventType, this.message, this.getTimestamp());
	}
	
	@Override
	public String toString() {
		return "OrderEvent [eventType=" + eventType + ", orderId=" + orderId + ", machineId=" + machineId + ", message="
				+ message + "]";
	}
}
