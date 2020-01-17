package fiab.mes.transport.msg;

public class InternalTransportModuleRequest {

	protected String capabilityInstanceIdFrom;
	protected String capabilityInstanceIdTo;
	protected String orderId;
	protected String requestId;
	
	public InternalTransportModuleRequest(String capabilityInstanceIdFrom, String capabilityInstanceIdTo, String orderId, String requestId) {
		super();
		this.capabilityInstanceIdFrom = capabilityInstanceIdFrom;
		this.capabilityInstanceIdTo = capabilityInstanceIdTo;
		this.orderId = orderId;
		this.requestId = requestId;
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
	public String getRequestId() {
		return requestId;
	}
	
}
