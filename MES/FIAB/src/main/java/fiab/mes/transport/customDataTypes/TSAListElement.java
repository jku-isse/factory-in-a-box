package fiab.mes.transport.customDataTypes;

import akka.actor.ActorRef;
import fiab.mes.transport.msg.RegisterTransportRequest;

public class TSAListElement {
	private RegisterTransportRequest rtr;
	private ActorRef sender;
	private int priority; // Unused at the moment

	public TSAListElement(RegisterTransportRequest rtr, ActorRef sender) {
		this.rtr = rtr;
		this.sender = sender;
		priority = 0;
	}

	public TSAListElement(RegisterTransportRequest rtr, ActorRef sender, int priority) {
		this.rtr = rtr;
		this.sender = sender;
		this.priority = priority;
	}

	public RegisterTransportRequest getRegisterTransportRequest() {
		return rtr;
	}

	public ActorRef getSender() {
		return sender;
	}

	public int getPriority() {
		return priority;
	}
}
