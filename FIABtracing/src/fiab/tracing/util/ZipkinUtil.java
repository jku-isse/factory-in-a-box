package fiab.tracing.util;

import brave.ScopedSpan;
import brave.Span;

public class ZipkinUtil {
	public static String createXB3Header(Span span) {
		StringBuilder sb = new StringBuilder();
		sb.append(span.context().traceIdString());
		sb.append("-");
		sb.append(span.context().spanIdString());
		sb.append("-");
		sb.append("d");
		sb.append("-");
		sb.append(span.context().parentIdString());
		return sb.toString();
	}

	public static void printSpan(Span span) {
		System.out.println("Span ID: " + span.context().spanIdString() + " Trace ID: " + span.context().traceIdString()
				+ " Parent ID: " + span.context().parentIdString());
	}

	public static String createXB3ScopeHeader(ScopedSpan span) {
		StringBuilder sb = new StringBuilder();
		sb.append(span.context().traceIdString());
		sb.append("-");
		sb.append(span.context().spanIdString());
		sb.append("-");
		sb.append("d");
		sb.append("-");
		sb.append(span.context().parentIdString());
		return sb.toString();
	}
}
