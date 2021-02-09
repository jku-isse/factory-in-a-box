package fiab.tracing.factory.impl;

import java.util.HashMap;

import brave.propagation.Propagation.Setter;
import fiab.tracing.actor.messages.ExtensibleMessage;

public class B3Setter<T> implements Setter<ExtensibleMessage<T>, String> {
	private final HashMap<String, String> map;

	public B3Setter() {
		map = new HashMap<String, String>();
	}

	@Override
	public void put(ExtensibleMessage<T> request, String key, String value) {
		map.put(key, value);
	}

}
