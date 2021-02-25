package fiab.turntable.actor;

import fiab.tracing.actor.messages.TracingHeader;

public class InternalTransportModuleRequest implements TracingHeader {

	private String header;

	protected String capabilityInstanceIdFrom;
	protected String capabilityInstanceIdTo;
	protected String orderId;
	protected String requestId;

	public InternalTransportModuleRequest(String capabilityInstanceIdFrom, String capabilityInstanceIdTo,
			String orderId, String requestId) {
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

	@Override
	public void setHeader(String header) {
		this.header = header;
	}

	@Override
	public String getHeader() {
		return header;
	}

}
