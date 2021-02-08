package fiab.tracing.factory;

import fiab.tracing.actor.messages.ExtensibleMessage;

public interface TracingFactory {
	public void createNewTrace(String traceName);

	public void injectMsg(ExtensibleMessage<Object> msg);

	public void startNewProdSpan(String spanName);

	public void startNewConSpan(String spanName);

	public void startProdSpan(ExtensibleMessage<? extends Object> msg, String spanName);

	public void startConSpan(ExtensibleMessage<? extends Object> msg, String spanName);

	public void finishCurrentSpan();

	public String getCurrentHeader();

}
