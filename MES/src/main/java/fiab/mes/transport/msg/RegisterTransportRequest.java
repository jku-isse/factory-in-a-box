package fiab.mes.transport.msg;

import akka.actor.ActorRef;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;

public class RegisterTransportRequest {
	
	private AkkaActorBackedCoreModelAbstractActor source; 
	private AkkaActorBackedCoreModelAbstractActor destination;
	private String orderId;
	private String transportId;
	private ActorRef requestor;
	
	public RegisterTransportRequest(AkkaActorBackedCoreModelAbstractActor from, AkkaActorBackedCoreModelAbstractActor to, String id, ActorRef requestor) {
		source = from;
		destination = to;
		this.orderId = id;
		this.requestor = requestor;
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
	
	public ActorRef getRequestor() {
		return requestor;
	}
}
