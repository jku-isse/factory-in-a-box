package fiab.tracing.factory.impl;

import brave.propagation.Propagation.Getter;
import fiab.tracing.actor.messages.ExtensibleMessage;

public class B3Getter<T> implements Getter<ExtensibleMessage<T>, String> {

	@Override
	public String get(ExtensibleMessage<T> request, String key) {
		return request.getHeader();
	}

}
