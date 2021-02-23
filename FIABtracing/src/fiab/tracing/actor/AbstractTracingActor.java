package fiab.tracing.actor;

import com.google.inject.Guice;
import com.google.inject.Injector;

import akka.actor.AbstractActor;
import fiab.tracing.config.Util;
import fiab.tracing.factory.TracingFactory;

public abstract class AbstractTracingActor extends AbstractActor {

	protected final TracingFactory tracingFactory;


	public AbstractTracingActor() {
		Injector injector = Guice.createInjector(Util.getConfig());
		tracingFactory = injector.getInstance(TracingFactory.class);
	}

	public TracingFactory getTracingFactory() {
		return tracingFactory;
	}

}
