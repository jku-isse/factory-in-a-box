package fiab.tracing.util;

import brave.Span;

public class ZipkinUtil {
	public static String createXB3Header(Span span) {
		StringBuilder sb = new StringBuilder();
		sb.append("b3: ");
		sb.append(span.context().traceIdString());
		sb.append("-");
		sb.append(span.context().parentIdString());
		sb.append("-");
		sb.append(span.context().spanIdString());

		return sb.toString();
	}

	public static void printSpan(Span span) {
		System.out.println("Span ID: " + span.context().spanIdString() + " Trace ID: " + span.context().traceIdString()
				+ " Parent ID: " + span.context().parentIdString());
	}
}
