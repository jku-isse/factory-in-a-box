package fiab.mes.order.msg;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import fiab.mes.general.TimedEvent;

public class OrderEvent extends TimedEvent {
	private OrderEventTypes eventType;
	private String orderId;
	private String machineId;
	
	public OrderEvent(String orderId, OrderEventTypes eventType, String machineId) {
		this.orderId = orderId;
		this.eventType = eventType;
		this.machineId = machineId;
		timestamp = ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
	}

	public OrderEventTypes getEventType() {
		return eventType;
	}

	public String getMachineId() {
		return machineId;
	}

	
	public String getOrderId() {
		return orderId;
	}
	
	
	
	
}

enum OrderEventTypes{ //TODO overthink using enums
	ORDER_AT_LOCATION("NO_LOCATION_YET"), ORDER_CANCELLED, ORDER_COMPLETED, ORDER_CREATED;
    private String info = "";
    OrderEventTypes(){}
    OrderEventTypes(String info){
       this.info = info;
    }
    public String getInfo(){
       return this.info;
    }
    public void setInfo(String info) {
    	this.info = info;
    }
    @Override
    public String toString() {
       return this.info;
    }
}
