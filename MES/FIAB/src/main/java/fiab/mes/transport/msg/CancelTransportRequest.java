
package fiab.mes.transport.msg;

public class CancelTransportRequest {
	protected String orderId;
	
	public CancelTransportRequest(String orderId) {
		super();
		this.orderId = orderId;
	}

	public String getOrderId() {
		return orderId;
	}
}
