package fiab.turntable;

import akka.actor.ActorSystem;
import fiab.tracing.impl.ZipkinTracing;
import fiab.tracing.util.Util;
import fiab.turntable.opcua.OPCUATurntableRootActor;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.urlconnection.URLConnectionSender;

public class StartupUtil {
	private final static AsyncReporter<zipkin2.Span> reporter;
	static {
		reporter = AsyncReporter.builder(URLConnectionSender.create(ZipkinTracing.getReportUrl())).build();
	}

	public static void startupWithExposedInternalControls(int portOffset, String name) {
		startup(portOffset, name, true);
	}

	public static void startupWithHiddenInternalControls(int portOffset, String name) {
		startup(portOffset, name, false);
	}

	private static void startup(int portOffset, String name, boolean exposeInternalControls) {
		ActorSystem system = ActorSystem.create("SYSTEM_" + name);
		system.registerExtension(Util.getTracingExtension());

		system.actorOf(OPCUATurntableRootActor.props(name, portOffset, exposeInternalControls, reporter),
				"TurntableRoot");
	}
}
