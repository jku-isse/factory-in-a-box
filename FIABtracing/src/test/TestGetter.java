package test;

import brave.propagation.Propagation.Getter;
import fiab.tracing.actor.messages.ExtensibleMessage;

public class TestGetter<T> implements Getter<ExtensibleMessage<T>, String> {

	@Override
	public String get(ExtensibleMessage<T> request, String key) {
		return request.getHeader();
	}

}
