package fiab.tracing.factory.impl;

import java.util.HashMap;

import brave.propagation.Propagation.Setter;
import fiab.tracing.actor.messages.AbstractExtensibleMessage;

public class B3Setter<T> implements Setter<AbstractExtensibleMessage<T>, String> {
	private final HashMap<String, String> map;

	public B3Setter() {
		map = new HashMap<String, String>();
	}

	@Override
	public void put(AbstractExtensibleMessage<T> request, String key, String value) {
		map.put(key, value);
	}

}
