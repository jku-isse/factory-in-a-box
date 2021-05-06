package fiab.turntable;

import akka.actor.ActorSystem;
import fiab.tracing.extension.TracingExtension;
import fiab.tracing.impl.zipkin.ZipkinTracing;
import fiab.turntable.opcua.OPCUATurntableRootActor;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.urlconnection.URLConnectionSender;

public class StartupUtil {
	private final static AsyncReporter<zipkin2.Span> reporter;
	// TODO remove for dynamically added URl
	static {
		reporter = AsyncReporter.builder(URLConnectionSender.create(ZipkinTracing.getReportUrl())).build();
	}

// Tracingextension can be null, if there is no certain extension used
	public static void startupWithExposedInternalControls(int portOffset, String name, TracingExtension ext) {
		startup(portOffset, name, true, ext);
	}

	public static void startupWithHiddenInternalControls(int portOffset, String name, TracingExtension ext) {
		startup(portOffset, name, false, ext);
	}

	private static void startup(int portOffset, String name, boolean exposeInternalControls, TracingExtension ext) {
		ActorSystem system = ActorSystem.create("SYSTEM_" + name);
		if (ext != null)
			system.registerExtension(ext);

		system.actorOf(OPCUATurntableRootActor.props(name, portOffset, exposeInternalControls, reporter),
				"TurntableRoot");
	}
}
