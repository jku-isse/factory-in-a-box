
package fiab.mes.transport.msg;

import fiab.tracing.actor.messages.TracingHeader;

public class CancelTransportRequest implements TracingHeader {
	private String header;

	protected String orderId;

	public CancelTransportRequest(String orderId, String header) {
		super();
		this.orderId = orderId;
		this.header = header;
	}

	public String getOrderId() {
		return orderId;
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
