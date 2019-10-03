package fiab.mes.transport.msg;

public class RegisterTransportRequest {
	
	private String fromMachine; //At the moment the expected Values are either TURNTABLE1 or TURNTABLE2
	private String toMachine;
	private String orderId;
	private String transportId; 
	
	public RegisterTransportRequest(String from, String to, String id) {
		fromMachine = from;
		toMachine = to;
		this.orderId = id;
	}
	
	public void setTransportId(String id) {
		transportId = id;
	}
	
	public String getTransportId() {
		return transportId;
	}

	public String getFromMachine() {
		return fromMachine;
	}

	public void setFromMachine(String fromMachine) {
		this.fromMachine = fromMachine;
	}

	public String getToMachine() {
		return toMachine;
	}

	public void setToMachine(String toMachine) {
		this.toMachine = toMachine;
	}

	public String getId() {
		return orderId;
	}

	public void setId(String id) {
		this.orderId = id;
	}
	
	

}
