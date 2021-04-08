package fiab.mes.order.msg;

import fiab.tracing.actor.messages.TracingHeader;

public class CancelOrTerminateOrder implements TracingHeader {
	private String header;

	protected String stepId;
	protected String rootOrderId;

	public String getStepId() {
		return stepId;
	}

	public void setStepId(String stepId) {
		this.stepId = stepId;
	}

	public String getRootOrderId() {
		return rootOrderId;
	}

	public void setRootOrderId(String rootOrderId) {
		this.rootOrderId = rootOrderId;
	}

	// cancel all steps for that order, regardless of stepId
	public CancelOrTerminateOrder(String orderId) {
		this(orderId, orderId, "");
	}

	public CancelOrTerminateOrder(String stepId, String rootOrderId) {
		this(stepId, rootOrderId, "");
	}

	public CancelOrTerminateOrder(String stepId, String rootOrderId, String header) {
		super();
		this.stepId = stepId;
		this.rootOrderId = rootOrderId;
		this.header = header;
	}

	@Override
	public void setTracingHeader(String header) {
		this.header = header;
	}

	@Override
	public String getTracingHeader() {
		return header;
	}
}
