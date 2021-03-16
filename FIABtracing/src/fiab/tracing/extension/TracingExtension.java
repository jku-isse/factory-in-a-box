package fiab.tracing.extension;

import com.google.inject.Injector;

import akka.actor.AbstractExtensionId;
import akka.actor.ExtendedActorSystem;
import akka.actor.Extension;
import akka.actor.ExtensionId;
import akka.actor.ExtensionIdProvider;
import fiab.tracing.extension.impl.TracingExtensionImpl;

public class TracingExtension extends AbstractExtensionId<TracingExtensionImpl> implements ExtensionIdProvider {
	private static TracingExtension TracingExtensionProvider;
	private final Injector injector;

	public TracingExtension(Injector injector) {
		this.injector = injector;
	}

	@Override
	public TracingExtensionImpl createExtension(ExtendedActorSystem system) {
		return new TracingExtensionImpl(injector);
	}

	@Override
	public ExtensionId<? extends Extension> lookup() {
		return getProvider();
	}

	public static void setProvider(TracingExtension ext) {
		TracingExtensionProvider = ext;
	}

	public static TracingExtension getProvider() {
		return TracingExtensionProvider;
	}

}
