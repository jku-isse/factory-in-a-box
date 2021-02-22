package fiab.tracing.factory;

import fiab.tracing.actor.messages.AbstractExtensibleMessage;

public interface TracingFactory {
	public void injectMsg(AbstractExtensibleMessage<? extends Object> msg);

	public void startProducerSpan(String spanName);

	public void startConsumerSpan(String spanName);

	public void startProducerSpan(AbstractExtensibleMessage<? extends Object> msg, String spanName);

	public void startConsumerSpan(AbstractExtensibleMessage<? extends Object> msg, String spanName);

	public void finishCurrentSpan();

	public String getCurrentHeader();

	public String getTraceId();

	public Object getCurrentSpan();

}
