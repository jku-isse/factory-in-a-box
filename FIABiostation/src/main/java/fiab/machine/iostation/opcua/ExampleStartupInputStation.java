package fiab.machine.iostation.opcua;

import fiab.core.capabilities.tracing.TestTracingUtil;
import fiab.tracing.extension.TracingExtension;

class ExampleStartupInputStation {
	public static void main(String[] args) {
		TracingExtension ext = TestTracingUtil.getTracingExtension();
		StartupUtil.startupInputstation(0, "VirtualInputStation1", ext);
	}
}
