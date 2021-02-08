package test;

import brave.propagation.Propagation.Setter;
import fiab.tracing.actor.messages.ExtensibleMessage;

public class TestSetter<T> implements Setter<ExtensibleMessage<T>,String>{

	@Override
	public void put(ExtensibleMessage<T> request, String header, String value) {
		request.setHeader(value);
	}

}
