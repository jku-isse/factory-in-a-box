package fiab.tracing.util;

import com.google.inject.AbstractModule;

import fiab.tracing.Traceability;
import fiab.tracing.impl.ZipkinTracing;

public class TestConfig extends AbstractModule {

	@Override
	protected void configure() {
		bind(Traceability.class).to(ZipkinTracing.class);
	}
}
