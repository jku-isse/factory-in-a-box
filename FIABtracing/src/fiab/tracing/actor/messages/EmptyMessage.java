package fiab.tracing.actor.messages;

public class EmptyMessage implements TracingHeader {

	@Override
	public void setTracingHeader(String header) {

	}

	@Override
	public String getTracingHeader() {
		return "";
	}

}
