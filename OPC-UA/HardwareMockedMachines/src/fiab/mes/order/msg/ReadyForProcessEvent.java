package fiab.mes.order.msg;

public class ReadyForProcessEvent {
	RegisterProcessStepRequest responseTo;
	boolean isReady = true;
		
	public ReadyForProcessEvent(RegisterProcessStepRequest responseTo) {
		super();
		this.responseTo = responseTo;
	}
	public ReadyForProcessEvent(RegisterProcessStepRequest responseTo, boolean isReady) {
		super();
		this.responseTo = responseTo;
		this.isReady = isReady;
	}
	public RegisterProcessStepRequest getResponseTo() {
		return responseTo;
	}
	public void setResponseTo(RegisterProcessStepRequest responseTo) {
		this.responseTo = responseTo;
	}
	public boolean isReady() {
		return isReady;
	}
	public void setReady(boolean isReady) {
		this.isReady = isReady;
	}
	
	
}