package fiab.capabilityManager.opcua.msg;

import fiab.tracing.actor.messages.TracingHeader;

public class ClientReadyNotification implements TracingHeader {

	private final String endpointUrl;
	private String header;

	public ClientReadyNotification(String endpointUrl, String header) {
		this.endpointUrl = endpointUrl;
		this.header = header;
	}

	public String getEndpointUrl() {
		return endpointUrl;
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
