package fiab.mes.order.msg;

import java.util.Optional;

import fiab.core.capabilities.events.TimedEvent;

public class ReadyForProcessEvent extends TimedEvent {
	RegisterProcessStepRequest responseTo;
	boolean isReady = true;
	ProcessRequestException pre;
	
	public ReadyForProcessEvent(RegisterProcessStepRequest responseTo) {
		super();
		this.responseTo = responseTo;
	}
	public ReadyForProcessEvent(RegisterProcessStepRequest responseTo, boolean isReady) {
		super();
		this.responseTo = responseTo;
		this.isReady = isReady;
	}
	public ReadyForProcessEvent(RegisterProcessStepRequest responseTo, ProcessRequestException pre ) {
		super();
		this.responseTo = responseTo;
		this.isReady = false;
		this.pre = pre;
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
	public Optional<ProcessRequestException> getProcessRequestException() {
		return Optional.ofNullable(pre);
	}
	
}
