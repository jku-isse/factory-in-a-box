package fiab.mes.tracing;

import com.google.inject.Guice;
import com.google.inject.Injector;

import fiab.tracing.extension.TracingExtension;

public class Util {
	private static TracingExtension tracingExt;
	static {
		Injector injector = Guice.createInjector(new TestConfig());
		tracingExt = new TracingExtension(injector);
		TracingExtension.setProvider(tracingExt);
	}

	public static TracingExtension getTracingExtension() {
		return tracingExt;
	}
	
	public void doSth() {
		
	}
}
