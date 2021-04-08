package fiab.mes.order.msg;

import akka.actor.ActorRef;
import fiab.tracing.actor.messages.TracingHeader;

public class RegisterProcessStepRequest implements TracingHeader {
	private String header;

	protected String rootOrderId;
	protected String processStepId;
	protected ProcessCore.ProcessStep step;
	protected ActorRef requestor;

	public RegisterProcessStepRequest(String rootOrderId, String processStepId, ProcessCore.ProcessStep step,
			ActorRef requestor) {
		this(rootOrderId,processStepId,step,requestor,"");
	}
	public RegisterProcessStepRequest(String rootOrderId, String processStepId, ProcessCore.ProcessStep step,
			ActorRef requestor,String header) {
		super();
		this.rootOrderId = rootOrderId;
		this.processStepId = processStepId;
		this.step = step;
		this.requestor = requestor;
		this.header = header;
	}


	public String getRootOrderId() {
		return rootOrderId;
	}

	public void setRootOrderId(String rootOrderId) {
		this.rootOrderId = rootOrderId;
	}

	public String getProcessStepId() {
		return processStepId;
	}

	public void setProcessStepId(String processStepId) {
		this.processStepId = processStepId;
	}

	public ProcessCore.ProcessStep getProcessStep() {
		return step;
	}

	public void setProcessStep(ProcessCore.ProcessStep step) {
		this.step = step;
	}

	public ActorRef getRequestor() {
		return requestor;
	}

	public void setRequestor(ActorRef requestor) {
		this.requestor = requestor;
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
