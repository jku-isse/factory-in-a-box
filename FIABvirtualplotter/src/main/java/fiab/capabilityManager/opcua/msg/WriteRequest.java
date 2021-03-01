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
	public void setHeader(String header) {
		this.header = header;
	}

	@Override
	public String getHeader() {
		return header;
	}
}
