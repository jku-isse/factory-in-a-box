package fiab.mes.transport.msg;

import akka.actor.ActorRef;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.tracing.actor.messages.TracingHeader;

public class RegisterTransportRequest implements TracingHeader {

	private AkkaActorBackedCoreModelAbstractActor source;
	private AkkaActorBackedCoreModelAbstractActor destination;
	private String orderId;
	private String transportId;
	private ActorRef requestor;
	private String header;

	public RegisterTransportRequest(AkkaActorBackedCoreModelAbstractActor from,
			AkkaActorBackedCoreModelAbstractActor to, String id, ActorRef requestor) {
		this(from, to, id, requestor, "");

	}

	public RegisterTransportRequest(AkkaActorBackedCoreModelAbstractActor from,
			AkkaActorBackedCoreModelAbstractActor to, String id, ActorRef requestor, String header) {
		source = from;
		destination = to;
		this.orderId = id;
		this.requestor = requestor;
		this.header = header;
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

	@Override
	public void setHeader(String header) {
		this.header = header;
	}

	@Override
	public String getHeader() {
		return header;
	}
}
