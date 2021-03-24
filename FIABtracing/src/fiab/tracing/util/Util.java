package fiab.tracing.util;

import com.google.inject.Guice;
import com.google.inject.Injector;

import fiab.tracing.extension.TracingExtension;

public class Util {
	private static TracingExtension tracingExt;
	private static Injector injector;
	static {
		 injector = Guice.createInjector(new TestConfig());
		tracingExt = new TracingExtension(injector);
		TracingExtension.setProvider(tracingExt);
	}

	public static TracingExtension getTracingExtension() {
		return new TracingExtension(injector);
	}
}
