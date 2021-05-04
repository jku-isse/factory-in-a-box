package fiab.core.capabilities.tracing;

import com.google.inject.Guice;
import com.google.inject.Injector;

import brave.ScopedSpan;
import brave.Span;
import brave.Tracing;
import fiab.tracing.actor.messages.TracingHeader;
import fiab.tracing.extension.TracingExtension;
import fiab.tracing.impl.zipkin.B3Setter;
import fiab.tracing.impl.zipkin.ZipkinTracing;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.brave.AsyncZipkinSpanHandler;
import zipkin2.reporter.brave.ZipkinSpanHandler;
import zipkin2.reporter.urlconnection.URLConnectionSender;

public class TestTracingUtil {
	private final static String REPORT_URL = "http://localhost:9411/api/v2/spans";
	private static Span span;
	private static TracingExtension tracingExt;
	private static URLConnectionSender sender;
	private static AsyncZipkinSpanHandler handler;
	private final static AsyncReporter<zipkin2.Span> reporter;
	private final static Tracing tracing;
	// TODO remove for dynamically added URl
	static {
		Injector injector = Guice.createInjector(new TestConfig());
		tracingExt = new TracingExtension(injector);
		TracingExtension.setProvider(tracingExt);

		sender = URLConnectionSender.create(REPORT_URL);
		handler = AsyncZipkinSpanHandler.create(sender).toBuilder().alwaysReportSpans(true).build();

		tracing = Tracing.newBuilder().localServiceName("Default").addSpanHandler(handler).build();

		reporter = AsyncReporter.builder(URLConnectionSender.create(ZipkinTracing.getReportUrl())).build();
	}

	public static Tracing createTracing(String serviceName) {
		return Tracing.newBuilder().localServiceName(serviceName).addSpanHandler(handler).build();
	}

	public static TracingExtension getTracingExtension() {
		return tracingExt;
	}

	public static AsyncReporter<zipkin2.Span> getReporter() {
		return reporter;
	}

	public static ScopedSpan createRandomSpanScope() {
		return tracing.tracer().startScopedSpan("DefaultScopedSpan");
	}

	public static brave.propagation.TraceContext.Injector<TracingHeader> getInjector() {
		return tracing.propagation().injector(new B3Setter());
	}

	public static Span createNewRandomSpan() {
		span = tracing.tracer().newTrace().name("RootSpan").start();
		return tracing.tracer().newChild(span.context());
	}

	public static void finishSpan() {
		span.finish();
		handler.close();
		sender.close();
	}

}
