package fiab.turntable.actor;

import fiab.tracing.actor.messages.TracingHeader;

public class InternalTransportModuleRequest implements TracingHeader {

	private String tracingHeader;

	protected String capabilityInstanceIdFrom;
	protected String capabilityInstanceIdTo;
	protected String orderId;
	protected String requestId;

	public InternalTransportModuleRequest(String capabilityInstanceIdFrom, String capabilityInstanceIdTo,
			String orderId, String requestId, String tracingHeader) {
		super();
		this.capabilityInstanceIdFrom = capabilityInstanceIdFrom;
		this.capabilityInstanceIdTo = capabilityInstanceIdTo;
		this.orderId = orderId;
		this.requestId = requestId;
		this.tracingHeader = tracingHeader;
	}

	public InternalTransportModuleRequest(String capabilityInstanceIdFrom, String capabilityInstanceIdTo,
			String orderId, String requestId) {
		this(capabilityInstanceIdFrom, capabilityInstanceIdTo, orderId, requestId, "");
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
	public void setTracingHeader(String header) {
		this.tracingHeader = header;
	}

	@Override
	public String getTracingHeader() {
		return tracingHeader;
	}

}
