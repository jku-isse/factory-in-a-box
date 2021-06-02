package fiab.tracing;

import fiab.tracing.actor.messages.TracingHeader;

public interface Traceability {
	public void initWithServiceName(String name);	

	/**
	 * @param msg interface for extracting header, pass NULL if no header is available
	 * @param spanName name of the span or tracing object
	 * */
	public void startProducerSpan(TracingHeader msg, String spanName);

	/**
	 * @param msg interface for extracting header, pass NULL if no header is available
	 * @param spanName name of the span or tracing object
	 * */
	public void startConsumerSpan(TracingHeader msg, String spanName);

	public void finishCurrentSpan();

	public void injectMsg(TracingHeader msg);

	public String getCurrentHeader();

	public void addAnnotation(String string);

	public void finish();

	public void startNewProcess(String string);




}
