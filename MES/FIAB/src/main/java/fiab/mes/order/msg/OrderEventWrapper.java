package fiab.mes.order.msg;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import fiab.mes.order.msg.OrderEvent.OrderEventType;

public class OrderEventWrapper {

	private OrderEventType eventType;
	private String orderId;
	private String machineId;
	private String message;
	private String timestamp;
	
	public OrderEventWrapper(OrderEvent e) {
		this.eventType = e.getEventType();
		try {
			this.orderId = URLEncoder.encode(e.getOrderId(), "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
			this.orderId = e.getOrderId();
		}
		try {
			this.machineId = URLEncoder.encode(e.getMachineId(), "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
			this.machineId = e.getMachineId();
		}
		this.timestamp = e.getTimestamp().toString();
		this.message = e.getMessage();
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
	
	public String getMessage() {
		return message;
	}
	
}
