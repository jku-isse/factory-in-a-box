package fiab.tracing.factory.impl;

import java.util.HashMap;
import java.util.Stack;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import fiab.tracing.factory.TracingFactory;
import zipkin2.reporter.brave.AsyncZipkinSpanHandler;
import zipkin2.reporter.brave.ZipkinSpanHandler;
import zipkin2.reporter.urlconnection.URLConnectionSender;

public class ZipkinFactory implements TracingFactory {
	private final URLConnectionSender sender;
	private final ZipkinSpanHandler handler;
	private final HashMap<String, Stack<Span>> tracingMap;

	public ZipkinFactory() {
		sender = URLConnectionSender.create("http://localhost:9411/api/v2/spans");
		handler = AsyncZipkinSpanHandler.create(sender).toBuilder().alwaysReportSpans(true).build();

		tracingMap = new HashMap<>();
	}
	
	@Override
	public Span createNewTrace(String traceName, String spanName) {
		Tracing tracing = Tracing.newBuilder().localServiceName(traceName).addSpanHandler(handler).build();	
		Span span = tracing.tracer().nextSpan().name(spanName);
		Stack<Span> stack = new Stack<>();
		stack.push(span);
		tracingMap.put(span.context().traceIdString(), stack);
		return span;
	}

}
