package fiab.tracing.factory.impl;

import brave.ScopedSpan;
import brave.Span;
import brave.Span.Kind;
import brave.Tracing;
import brave.propagation.TraceContext.Extractor;
import brave.propagation.TraceContext.Injector;
import fiab.tracing.actor.messages.ExtensibleMessage;
import fiab.tracing.factory.TracingFactory;
import fiab.tracing.util.ZipkinUtil;
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

	@Override
	public void finishCurrentSpan() {
		try {
			currentSpan.finish();
			printSpanFinished();
			currentSpan = null;
		} catch (NullPointerException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void startProducerSpan(String spanName) {
		currentSpan = tracing.tracer().nextSpan().name(spanName).kind(Kind.PRODUCER).start();
		printSpanStarted();
	}

	@Override
	public void startConsumerSpan(String spanName) {
		currentSpan = tracing.tracer().newChild(scope.context()).kind(Kind.CONSUMER).name(spanName).start();
		printSpanStarted();
	}

	@Override
	public void startProducerSpan(ExtensibleMessage<? extends Object> msg, String spanName) {
		currentSpan = tracing.tracer().nextSpan(extractor.extract((ExtensibleMessage<Object>) msg)).name(spanName)
				.kind(Kind.PRODUCER).start();
		printSpanStarted();
	}

	@Override
	public void startConsumerSpan(ExtensibleMessage<? extends Object> msg, String spanName) {
		currentSpan = tracing.tracer().nextSpan(extractor.extract((ExtensibleMessage<Object>) msg)).name(spanName)
				.kind(Kind.CONSUMER).start();
		printSpanStarted();
	}

	@Override
	public void injectMsg(ExtensibleMessage<? extends Object> msg) {
		injector.inject(currentSpan.context(), (ExtensibleMessage<Object>) msg);
	}

	@Override
	public String getCurrentHeader() {
		StringBuilder sb = new StringBuilder();
		sb.append(currentSpan.context().traceIdString());
		sb.append("-");
		sb.append(currentSpan.context().spanIdString());
		sb.append("-");
		sb.append("d");
		sb.append("-");
		sb.append(currentSpan.context().parentIdString());
		return sb.toString();
	}

	@Override
	public String getTraceId() {
		return scope == null ? "" : scope.context().traceIdString();
	}

	private void printSpanStarted() {
		System.out.print("New Span started: ");
		ZipkinUtil.printSpan(currentSpan);

	}

	private void printSpanFinished() {
		System.out.print("Span finished: ");
		ZipkinUtil.printSpan(currentSpan);
	}

	@Override
	public Object getCurrentSpan() {
		return currentSpan;
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
