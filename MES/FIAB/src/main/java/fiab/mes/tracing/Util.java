package fiab.mes.tracing;

import com.google.inject.Guice;
import com.google.inject.Injector;

import fiab.tracing.extension.TracingExtension;
import fiab.tracing.impl.zipkin.ZipkinTracing;
import zipkin2.Span;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.urlconnection.URLConnectionSender;

public class Util {

	private final static AsyncReporter<zipkin2.Span> reporter;
	// TODO remove for dynamically added URl
	static {
		reporter = AsyncReporter.builder(URLConnectionSender.create(ZipkinTracing.getReportUrl())).build();
	}

	public static AsyncReporter<Span> getReporter() {
		return reporter;
	}
}
