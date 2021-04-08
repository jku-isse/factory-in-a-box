package fiab.tracing.impl.zipkin;

import java.util.Random;

import brave.ScopedSpan;
import brave.Span;

public class ZipkinUtil {
	public static void main(String[] args) {
		System.out.println(generateRandomB3Header());
	}

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

	public static String extractTraceId(String tracingHeader) {
		return tracingHeader.split("-")[0];
	}

	public static String extractParentId(String tracingHeader) {
		String[] split = tracingHeader.split("-");
		return split[split.length - 1];
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

	public static String createB3Header(String spanId, String traceId, String parentId) {
		StringBuilder sb = new StringBuilder();
		sb.append(traceId);
		sb.append("-");
		sb.append(spanId);
		sb.append("-");
		sb.append("d");
		sb.append("-");
		sb.append(parentId);
		return sb.toString();
	}

	public static String generateRandomB3Header() {
		StringBuilder sb = new StringBuilder();
		sb.append(generate16BitHexNumber());
		sb.append("-");
		sb.append(generate16BitHexNumber());
		sb.append("-");
		sb.append("d");
		sb.append("-");
		sb.append(generate16BitHexNumber());
		return sb.toString();
	}

	private static String generate16BitHexNumber() {
		Random r = new Random();
		return String.format("%16x", r.nextLong());
	}
}
