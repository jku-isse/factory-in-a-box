package fiab.capabilityManager.opcua.msg;

import fiab.tracing.actor.messages.TracingHeader;

public class WriteRequest implements TracingHeader {

	private final String data;
	private String header;

	public WriteRequest(String data, String header) {
		this.data = data;
		this.header = header;
	}

	public String getData() {
		return data;
	}

	@Override
	public void setTracingHeader(String header) {
		this.header = header;
	}

	@Override
	public String getTracingHeader() {
		return header;
	}
}
