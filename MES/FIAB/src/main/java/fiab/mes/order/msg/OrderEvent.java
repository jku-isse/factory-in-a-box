package fiab.mes.order.msg;

import fiab.mes.general.TimedEvent;

public class OrderEvent extends TimedEvent {
	private OrderEventType eventType;
	private String orderId;
	private String machineId;
	
	public OrderEvent(String orderId, String machineId, OrderEventType eventType) {
		super();
		this.orderId = orderId;
		this.eventType = eventType;
		this.machineId = machineId;
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

	public static enum OrderEventType {
		CREATED, REGISTERED, SCHEDULED, ALLOCATED, PRODUCTION_UPDATE, TRANSPORT_UPDATE, CANCELED, PAUSED, CONTINUED, COMPLETED, DELETED
	}

}

