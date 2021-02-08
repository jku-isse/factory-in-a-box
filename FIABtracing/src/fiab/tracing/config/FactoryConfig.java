package fiab.tracing.config;

import com.google.inject.AbstractModule;

import fiab.tracing.factory.TracingFactory;
import fiab.tracing.factory.impl.ZipkinFactory;

public class FactoryConfig extends AbstractModule {

	@Override
	protected void configure() {
		configureTracingFactoryBinding();

	}

	private void configureTracingFactoryBinding() {
		bind(TracingFactory.class).toInstance(new ZipkinFactory());
	}

}
