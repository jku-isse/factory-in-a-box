
package fiab.mes.transport.msg;

public class CancelTransportRequest {
	private String orderId;
	
	public CancelTransportRequest(String orderId) {
		this.orderId = orderId;
	}
	
	public String getOrderId() {
		return orderId;
	}
}
