package fiab.tracing.impl;

import java.util.HashMap;

import brave.propagation.Propagation.Setter;
import fiab.tracing.actor.messages.TracingHeader;

public class B3Setter implements Setter<TracingHeader, String> {
	private final HashMap<String, String> map;

	public B3Setter() {
		map = new HashMap<String, String>();
	}

	@Override
	public void put(TracingHeader request, String key, String value) {
		map.put(key, value);
	}

}
