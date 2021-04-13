package fiab.core.capabilities.tracing;

import com.google.inject.Guice;
import com.google.inject.Injector;

import fiab.tracing.extension.TracingExtension;
import fiab.tracing.impl.zipkin.ZipkinTracing;
import zipkin2.Span;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.urlconnection.URLConnectionSender;

public class TestTracingUtil {
	private static TracingExtension tracingExt;
	private final static AsyncReporter<zipkin2.Span> reporter;
	// TODO remove for dynamically added URl
	static {
		Injector injector = Guice.createInjector(new TestConfig());
		tracingExt = new TracingExtension(injector);
		TracingExtension.setProvider(tracingExt);
		reporter = AsyncReporter.builder(URLConnectionSender.create(ZipkinTracing.getReportUrl())).build();
	}

	public static TracingExtension getTracingExtension() {
		return tracingExt;
	}

	public static AsyncReporter<Span> getReporter() {
		return reporter;
	}

}
