package fiab.tracing;

import fiab.tracing.actor.messages.TracingHeader;

public interface Traceability {
	public void initWithServiceName(String name);

	public void startProducerSpan(String spanName);

	public void startConsumerSpan(String spanName);

	public void startProducerSpan(TracingHeader msg, String spanName);

	public void startConsumerSpan(TracingHeader msg, String spanName);

	public void finishCurrentSpan();

	public void injectMsg(TracingHeader msg);

	public String getCurrentHeader();

	public Object getCurrentSpan();

}
