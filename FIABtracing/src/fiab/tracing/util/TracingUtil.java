package fiab.tracing.util;

import fiab.tracing.Traceability;
import fiab.tracing.impl.LogTracing;

public class TracingUtil {

	public static Traceability getDefaultLoggingTracer() {		
		return new LogTracing();
	}
}
