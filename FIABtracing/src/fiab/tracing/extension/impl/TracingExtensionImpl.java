package fiab.tracing.extension.impl;

import com.google.inject.Injector;

import akka.actor.Extension;

public class TracingExtensionImpl implements Extension {
	private Injector injector;

	public TracingExtensionImpl(Injector injector) {
		this.injector = injector;
	}

	public Injector getInjector() {
		return injector;
	}
}
