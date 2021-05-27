package fiab.tracing.impl;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fiab.tracing.Traceability;
import fiab.tracing.actor.messages.TracingHeader;

public class LogTracing implements Traceability {
	private static final Logger logger = LoggerFactory.getLogger(LogTracing.class);
	
	private String currentHeader;

	@Override
	public void initWithServiceName(String name) {		

	}

	@Override
	public void startProducerSpan(TracingHeader msg, String spanName) {
		logger.info("Started Producer Span: " + LocalDateTime.now() + " " + spanName);
		currentHeader = msg.getTracingHeader();
	}

	@Override
	public void startConsumerSpan(TracingHeader msg, String spanName) {
		logger.info("Started Consumer Span: " + LocalDateTime.now() + " " + spanName);
		currentHeader = msg.getTracingHeader();
	}

	@Override
	public void finishCurrentSpan() {		

	}

	@Override
	public void injectMsg(TracingHeader msg) {	

	}

	@Override
	public String getCurrentHeader() {		
		return currentHeader;
	}

	@Override
	public void addAnnotation(String string) {
		// TODO Auto-generated method stub
		
	}

}
