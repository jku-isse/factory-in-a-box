package fiab.tracing.actor;

import com.google.inject.Injector;

import akka.actor.AbstractActor;
import fiab.tracing.Traceability;
import fiab.tracing.extension.TracingExtension;
import fiab.tracing.util.TracingUtil;

public abstract class AbstractTracingActor extends AbstractActor {

	protected Traceability tracer;
	private Injector injector;

	public AbstractTracingActor() {
		super();
		try {
			injector = TracingExtension.getProvider().get(getContext().getSystem()).getInjector();
			tracer = injector.getInstance(Traceability.class);
			tracer.initWithServiceName(getSelf().path().name());
		} catch (NullPointerException e) {
			tracer = TracingUtil.getDefaultLoggingTracer();
		}
	}
}
