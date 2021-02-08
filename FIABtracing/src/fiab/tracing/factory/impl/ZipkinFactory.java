package fiab.tracing.factory.impl;

import java.util.HashMap;
import java.util.Stack;

import brave.ScopedSpan;
import brave.Span;
import brave.Span.Kind;
import brave.propagation.TraceContext.Extractor;
import brave.propagation.TraceContext.Injector;
import brave.Tracer;
import brave.Tracing;
import fiab.tracing.actor.messages.ExtensibleMessage;
import fiab.tracing.factory.TracingFactory;
import test.TestGetter;
import test.TestSetter;
import zipkin2.reporter.brave.AsyncZipkinSpanHandler;
import zipkin2.reporter.brave.ZipkinSpanHandler;
import zipkin2.reporter.urlconnection.URLConnectionSender;

public class ZipkinFactory implements TracingFactory {
	private final URLConnectionSender sender;
	private final ZipkinSpanHandler handler;
	private static Extractor<ExtensibleMessage<Object>> extractor;
	private static Injector<ExtensibleMessage<Object>> injector;

	private Tracing tracing;
	private ScopedSpan scope;
	private Span currentSpan;

	static {

	}

	public ZipkinFactory() {
		sender = URLConnectionSender.create("http://localhost:9411/api/v2/spans");
		handler = AsyncZipkinSpanHandler.create(sender).toBuilder().alwaysReportSpans(true).build();
		tracing = Tracing.newBuilder().localServiceName("FIAB Tracing").addSpanHandler(handler).build();
		if (extractor == null)
			extractor = tracing.propagation().extractor(new B3Getter<>());

		if (injector == null)
			injector = tracing.propagation().injector(new B3Setter<>());
	}

	@Override
	public void createNewTrace(String traceName) {
		if (scope == null)
			scope = createNewScope(traceName, "Zipkin TestScope");
	}

	private ScopedSpan createNewScope(String traceName, String scopeName) {
		tracing = Tracing.newBuilder().localServiceName(traceName).addSpanHandler(handler).build();
		return tracing.tracer().startScopedSpan(scopeName);

	}

	public void createNewTrace(String traceName, String scopeName) {
		tracing = Tracing.newBuilder().localServiceName(traceName).addSpanHandler(handler).build();
		tracing.tracer().startScopedSpan(scopeName);
	}

	@Override
	public void finishCurrentSpan() {
		try {
			currentSpan.finish();
			System.out.println("CurrentSpan: " + currentSpan.context().traceIdString());
			currentSpan = null;
		} catch (NullPointerException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void startNewProdSpan(String spanName) {
		currentSpan = tracing.tracer().nextSpan().name(spanName).kind(Kind.PRODUCER).start();
	}

	@Override
	public void startNewConSpan(String spanName) {
		currentSpan = tracing.tracer().nextSpan().name(spanName).kind(Kind.CONSUMER).start();
	}

	@Override
	public void startProdSpan(ExtensibleMessage<? extends Object> msg, String spanName) {
		currentSpan = tracing.tracer().nextSpan(extractor.extract((ExtensibleMessage<Object>) msg)).name(spanName).kind(Kind.PRODUCER).start();

	}

	@Override
	public void startConSpan(ExtensibleMessage<? extends Object> msg, String spanName) {
		currentSpan = tracing.tracer().nextSpan(extractor.extract((ExtensibleMessage<Object>) msg)).name(spanName).kind(Kind.CONSUMER).start();

	}

	@Override
	public void injectMsg(ExtensibleMessage<Object> msg) {
		injector.inject(currentSpan.context(), msg);
	}

	@Override
	public String getCurrentHeader() {
		// TODO Auto-generated method stub
		return null;
	}
	
	

//	@Override
//	public Span createNewTrace(String traceName, String spanName) {
//		Tracing tracing = Tracing.newBuilder().localServiceName(traceName).addSpanHandler(handler).build();	
//		Span span = tracing.tracer().nextSpan().name(spanName);
//		Stack<Span> stack = new Stack<>();
//		stack.push(span);
//		tracingMap.put(span.context().traceIdString(), stack);
//		return span;
//	}

}
