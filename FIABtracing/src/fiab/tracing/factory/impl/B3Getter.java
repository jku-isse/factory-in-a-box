package fiab.tracing.factory.impl;

import brave.propagation.Propagation.Getter;
import fiab.tracing.actor.messages.AbstractExtensibleMessage;

public class B3Getter<T> implements Getter<AbstractExtensibleMessage<T>, String> {

	@Override
	public String get(AbstractExtensibleMessage<T> request, String key) {
		return request.getHeader();
	}

}
