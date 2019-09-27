package fiab.mes.order.msg;

import fiab.mes.order.msg.OrderEvent.OrderEventType;

public class OrderEventWrapper {

	private OrderEventType eventType;
	private String orderId;
	private String machineId;
	private String timestamp;
	
	public OrderEventWrapper(OrderEvent e) {
		this.eventType = e.getEventType();
		this.orderId = e.getOrderId();
		this.machineId = e.getMachineId();
		this.timestamp = e.getTimestamp().toString();
	}

	public OrderEventType getEventType() {
		return eventType;
	}

	public String getOrderId() {
		return orderId;
	}

	public String getMachineId() {
		return machineId;
	}

	public String getTimestamp() {
		return timestamp;
	}
	
	
}
