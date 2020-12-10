package fiab.tracing.factory;

import brave.Span;

public interface TracingFactory {
	public Span createNewTrace(String string1,String string2);
	
}
