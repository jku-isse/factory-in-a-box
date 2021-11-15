package fiab.mes.order.msg;

public class CancelOrTerminateOrder {
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
		super();
		this.stepId = null;
		this.rootOrderId = orderId;
	}	
	
	public CancelOrTerminateOrder(String stepId, String rootOrderId) {
		super();
		this.stepId = stepId;
		this.rootOrderId = rootOrderId;
	}
}
