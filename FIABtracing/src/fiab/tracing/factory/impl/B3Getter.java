package fiab.tracing.factory.impl;

import brave.propagation.Propagation.Getter;
import fiab.tracing.actor.messages.ExtensibleMessage;
import fiab.tracing.actor.messages.TracingHeader;

public class B3Getter implements Getter<TracingHeader, String> {

	@Override
	public String get(TracingHeader request, String key) {
		return request.getHeader();
	}

}
