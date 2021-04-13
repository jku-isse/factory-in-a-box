package fiab.machine.iostation.opcua;


import fiab.tracing.extension.TracingExtension;

class ExampleStartupInputStation {
	public static void main(String[] args) {
//		TracingExtension ext = TestTracingUtil.getTracingExtension();
		StartupUtil.startupInputstation(0, "VirtualInputStation1", null);
	}
}
