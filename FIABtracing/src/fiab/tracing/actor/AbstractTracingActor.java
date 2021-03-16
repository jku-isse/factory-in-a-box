package fiab.tracing.actor;

import com.google.inject.Injector;

import akka.actor.AbstractActor;
import fiab.tracing.Traceability;
import fiab.tracing.extension.TracingExtension;

public abstract class AbstractTracingActor extends AbstractActor {

	protected final Traceability tracer;
	protected Injector injector;

	public AbstractTracingActor() {
		super();
		injector = TracingExtension.getProvider().get(getContext().getSystem()).getInjector();
		tracer = injector.getInstance(Traceability.class);
		tracer.initWithServiceName(getSelf().path().name());
	}

	public Traceability getTracingFactory() {
		return tracer;
	}

}
