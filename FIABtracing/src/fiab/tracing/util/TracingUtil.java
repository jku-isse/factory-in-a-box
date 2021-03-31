package fiab.tracing.util;

import com.google.inject.Guice;
import com.google.inject.Injector;

import fiab.tracing.Traceability;
import fiab.tracing.actor.AbstractTracingActor;
import fiab.tracing.extension.TracingExtension;
import fiab.tracing.impl.LogTracing;

public class TracingUtil {
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

	public static Traceability getDefaultLoggingTracer() {		
		return new LogTracing();
	}
}
