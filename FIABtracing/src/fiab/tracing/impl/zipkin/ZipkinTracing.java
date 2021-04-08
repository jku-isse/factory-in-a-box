package fiab.tracing.impl.zipkin;

import brave.ScopedSpan;
import brave.Span;
import brave.Span.Kind;
import brave.Tracing;
import brave.propagation.TraceContext.Extractor;
import brave.propagation.TraceContext.Injector;
import fiab.tracing.Traceability;
import fiab.tracing.actor.messages.TracingHeader;
import zipkin2.reporter.brave.AsyncZipkinSpanHandler;
import zipkin2.reporter.brave.ZipkinSpanHandler;
import zipkin2.reporter.urlconnection.URLConnectionSender;

public class ZipkinTracing implements Traceability {
	private final static String REPORT_URL = "http://localhost:9411/api/v2/spans";
	private final static URLConnectionSender sender;
	private final static ZipkinSpanHandler handler;

	private Extractor<TracingHeader> extractor;
	private Injector<TracingHeader> injector;
	private ScopedSpan scope;

	private Tracing tracing;
	private Span currentSpan;

	static {
		sender = URLConnectionSender.create(REPORT_URL);
		handler = AsyncZipkinSpanHandler.create(sender).toBuilder().alwaysReportSpans(true).build();
	}

	@Override
	public void initWithServiceName(String localServiceName) {
		tracing = Tracing.newBuilder().localServiceName(localServiceName).addSpanHandler(handler).build();
		if (extractor == null)
			extractor = tracing.propagation().extractor(new B3Getter());

		if (injector == null)
			injector = tracing.propagation().injector(new B3Setter());
		scope = createNewScope(localServiceName, "Default");
	}

	
	public void startProducerSpan(String spanName) {
		currentSpan = tracing.tracer().nextSpan().name(spanName).kind(Kind.PRODUCER).start();
	}

	
	public void startConsumerSpan(String spanName) {
		currentSpan = tracing.tracer().newChild(scope.context()).kind(Kind.CONSUMER).name(spanName).start();
	}

	@Override
	public void startProducerSpan(TracingHeader msg, String spanName) {
		if (msg.getTracingHeader().isEmpty())
			startProducerSpan(spanName);
		else {
			currentSpan = tracing.tracer().nextSpan(extractor.extract(msg)).name(spanName).kind(Kind.PRODUCER).start();
		}
		printSpanStarted();
	}

	@Override
	public void startConsumerSpan(TracingHeader msg, String spanName) {
		if (msg.getTracingHeader().isEmpty())
			startConsumerSpan(spanName);
		else {
			currentSpan = tracing.tracer().nextSpan(extractor.extract(msg)).name(spanName).kind(Kind.CONSUMER).start();
		}
		printSpanStarted();
	}

	@Override
	public void finishCurrentSpan() {
		try {
			currentSpan.finish();
			printSpanFinished();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void injectMsg(TracingHeader msg) {
		try {
			injector.inject(currentSpan.context(), msg);
		} catch (Exception e) {
			injector.inject(scope.context(), msg);
		}
	}

	@Override
	public String getCurrentHeader() {
		if (currentSpan == null)
			return scopedSpanHeader();
		else
			return currentSpanHeader();
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

	private ScopedSpan createNewScope(String traceName, String scopeName) {
		return tracing.tracer().startScopedSpan(scopeName);
	}

	private String currentSpanHeader() {
		return ZipkinUtil.createXB3Header(currentSpan);
	}

	private String scopedSpanHeader() {
		return ZipkinUtil.createXB3ScopeHeader(scope);
	}

	private void printSpanStarted() {
//		System.out.print("New Span started: ");
//		ZipkinUtil.printSpan(currentSpan);

	}
	

	private void printSpanFinished() {
//		System.out.print("Span finished: ");
//		ZipkinUtil.printSpan(currentSpan);
	}
	
	public static String getReportUrl() {
		return REPORT_URL;
	}

}
