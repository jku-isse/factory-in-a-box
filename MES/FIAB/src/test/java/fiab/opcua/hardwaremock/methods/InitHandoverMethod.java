package fiab.opcua.hardwaremock.methods;

import akka.actor.ActorRef;
import fiab.mes.transport.handshake.HandshakeProtocol;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerMessageTypes;

public class InitHandoverMethod extends Methods {
private ActorRef actor;
	
	public InitHandoverMethod(ActorRef actor) {
		this.actor = actor;
	}

	@Override
	public void invoke() {
		System.out.println("INIT_HANDOVER GOT INVOKED");
		actor.tell(HandshakeProtocol.ServerMessageTypes.RequestInitiateHandover, ActorRef.noSender()); //TODO check if this is the request
	}
	
	@Override
	public String getInfo() {
		return "This method initializes the Handover!";
	}

}