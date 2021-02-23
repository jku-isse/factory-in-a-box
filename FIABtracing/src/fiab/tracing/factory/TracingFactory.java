package fiab.tracing.factory;

import fiab.tracing.actor.messages.TracingHeader;

public interface TracingFactory {
	public void injectMsg(TracingHeader msg);

	public void startProducerSpan(String spanName);

	public void startConsumerSpan(String spanName);

	public void startProducerSpan(TracingHeader msg, String spanName);

	public void startConsumerSpan(TracingHeader msg, String spanName);

	public void finishCurrentSpan();

	public String getCurrentHeader();

	public String getTraceId();

	public Object getCurrentSpan();

}
