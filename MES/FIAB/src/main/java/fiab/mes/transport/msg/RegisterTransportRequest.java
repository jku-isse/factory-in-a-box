package fiab.mes.transport.msg;

import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;

public class RegisterTransportRequest {
	
	private AkkaActorBackedCoreModelAbstractActor source; 
	private AkkaActorBackedCoreModelAbstractActor destination;
	private String orderId;
	private String transportId; 
	
	public RegisterTransportRequest(AkkaActorBackedCoreModelAbstractActor from, AkkaActorBackedCoreModelAbstractActor to, String id) {
		source = from;
		destination = to;
		this.orderId = id;
	}
	
	public void setTransportId(String id) {
		transportId = id;
	}
	
	public String getTransportId() {
		return transportId;
	}

	public String getOrderId() {
		return orderId;
	}

	public AkkaActorBackedCoreModelAbstractActor getSource() {
		return source;
	}

	public AkkaActorBackedCoreModelAbstractActor getDestination() {
		return destination;
	}
}
