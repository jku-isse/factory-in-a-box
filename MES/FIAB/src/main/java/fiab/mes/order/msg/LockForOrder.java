package fiab.mes.order.msg;

import fiab.tracing.actor.messages.TracingHeader;

public class LockForOrder implements TracingHeader {
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

	public LockForOrder(String stepId) {
		this(stepId, stepId, "");
	}

	public LockForOrder(String stepId, String rootOrderId) {
		this(stepId, rootOrderId, "");
	}

	public LockForOrder(String stepId, String rootOrderId, String header) {
		super();
		this.stepId = stepId;
		this.rootOrderId = rootOrderId;
		this.header = header;
	}

	@Override
	public String toString() {
		return "LockForOrder [stepId=" + stepId + ", rootOrderId=" + rootOrderId + "]";
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
