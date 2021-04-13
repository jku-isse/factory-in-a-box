package fiab.core.capabilities.tracing;

import com.google.inject.AbstractModule;

import fiab.tracing.Traceability;
import fiab.tracing.impl.zipkin.ZipkinTracing;

public class TestConfig extends AbstractModule {

	@Override
	protected void configure() {
		bind(Traceability.class).to(ZipkinTracing.class);
	}
}
