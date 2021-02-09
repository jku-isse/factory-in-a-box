package fiab.tracing.factory;

import fiab.tracing.actor.messages.ExtensibleMessage;

public interface TracingFactory {
	public void createNewTrace(String traceName);

	public void injectMsg(ExtensibleMessage<? extends Object> msg );

	public void startProducerSpan(String spanName);

	public void startConsumerSpan(String spanName);

	public void startProducerSpan(ExtensibleMessage<? extends Object> msg, String spanName);

	public void startConsumerSpan(ExtensibleMessage<? extends Object> msg, String spanName);

	public void finishCurrentSpan();

	public String getCurrentHeader();
	
	public String getTraceId();
	
	public Object getCurrentSpan();

}
