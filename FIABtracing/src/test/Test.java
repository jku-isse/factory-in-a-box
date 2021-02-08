package test;

import com.google.inject.Guice;
import com.google.inject.Inject;

import brave.ScopedSpan;
import brave.Span;
import brave.Tracing;
import brave.propagation.TraceContext.Extractor;
import brave.propagation.TraceContext.Injector;
import fiab.tracing.actor.messages.ExtensibleMessage;
import fiab.tracing.config.Util;
import fiab.tracing.factory.TracingFactory;
import zipkin2.reporter.brave.AsyncZipkinSpanHandler;
import zipkin2.reporter.brave.ZipkinSpanHandler;
import zipkin2.reporter.urlconnection.URLConnectionSender;

public class Test {
	
	
	TracingFactory fac;
	

	
	public static void main(String[] args) {
		
		
		new Test().testInjection();

	}

//	private void start() {
//		URLConnectionSender sender = URLConnectionSender.create("http://localhost:9411/api/v2/spans");
//		ZipkinSpanHandler handler = AsyncZipkinSpanHandler.create(sender).toBuilder().alwaysReportSpans(true).build();
//		Tracing tracing = Tracing.newBuilder().localServiceName("test").addSpanHandler(handler).build();
//
//		Factory test = B3Propagation.newFactoryBuilder().injectFormat(Format.SINGLE).build();
//	
//
//		ScopedSpan span = tracing.tracer().startScopedSpan("Producer");
//
//		for (int i = 0; i < 10000; i++) {
//			System.out.println(i);
//		}
//		System.out.println(tracing.tracer().currentSpan());
//
//		Span span2 = tracing.tracer().nextSpan().name("Producer").kind(Kind.PRODUCER).start();
//		
//		tracing.propagation().injector(TestRequest::addHeader);
//
//		for (int i = 0; i < 50000; i++) {
//			System.out.println(i);
//		}
//		span2.finish();
//
//		span2 = tracing.tracer().nextSpan().name("Consumer2").kind(Kind.CONSUMER).start();
//
//		for (int i = 0; i < 50000; i++) {
//			System.out.println(i);
//		}
//		span2.finish();
//
//		System.out.println(span.context().traceIdString());
//		System.out.println(span2.context().traceIdString());
//		tracing.close();
//		handler.close();
//		sender.close();
//	}

	private void testInjection() {
		 fac = Guice.createInjector(Util.getConfig()).getInstance(TracingFactory.class);
		System.out.println("asdawerdasdf");
	}

	private void start() {
		URLConnectionSender sender = URLConnectionSender.create("http://localhost:9411/api/v2/spans");
		ZipkinSpanHandler handler = AsyncZipkinSpanHandler.create(sender).toBuilder().alwaysReportSpans(true).build();
		Tracing tracing = Tracing.newBuilder().localServiceName("test").addSpanHandler(handler).build();

		Message msg = new Message("asd");

		ScopedSpan scope = tracing.tracer().startScopedSpan("Test");

		Injector<ExtensibleMessage<Object>> injector = tracing.propagation().injector(new TestSetter<>());
		injector.inject(tracing.currentTraceContext().get(), msg);

		Extractor<ExtensibleMessage<Object>> extractor = tracing.propagation().extractor(new TestGetter<>());
		Span span = tracing.tracer().nextSpan(extractor.extract(msg));


		span.start();
		for (int i = 0; i < 50000; i++) {
			System.out.println(i);
		}
		span.finish();

		scope.finish();
		
		System.out.println(span.context().traceIdString());
		System.out.println(scope.context().traceIdString());
		tracing.close();
		handler.close();
//		sender.close();
	}
}
