package fiab.mes.order.msg;

public class LockForOrder {
	protected String stepId;	
	protected String rootOrderId;
	
	public String getStepId() {
		return stepId;
	}

	public void setStepId(String stepId) {
		this.stepId = stepId;
	}

	public LockForOrder(String stepId) {
		super();
		this.stepId = stepId;
		this.rootOrderId = stepId;
	}	
	
	public LockForOrder(String stepId, String rootOrderId) {
		super();
		this.stepId = stepId;
		this.rootOrderId = rootOrderId;
	}
}
