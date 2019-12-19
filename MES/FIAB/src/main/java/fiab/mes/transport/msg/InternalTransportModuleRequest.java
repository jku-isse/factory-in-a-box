package fiab.mes.transport.msg;

public class InternalTransportModuleRequest {

	protected String capabilityInstanceIdFrom;
	protected String capabilityInstanceIdTo;
	protected String orderId;
	
	public InternalTransportModuleRequest(String capabilityInstanceIdFrom, String capabilityInstanceIdTo, String orderId) {
		super();
		this.capabilityInstanceIdFrom = capabilityInstanceIdFrom;
		this.capabilityInstanceIdTo = capabilityInstanceIdTo;
		this.orderId = orderId;
	}
	
	public String getCapabilityInstanceIdFrom() {
		return capabilityInstanceIdFrom;
	}
	public String getCapabilityInstanceIdTo() {
		return capabilityInstanceIdTo;
	}
	public String getOrderId() {
		return orderId;
	}
	
	
}
