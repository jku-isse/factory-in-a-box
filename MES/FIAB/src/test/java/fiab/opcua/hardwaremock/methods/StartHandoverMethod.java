package fiab.opcua.hardwaremock.methods;

import akka.actor.ActorRef;
import fiab.mes.transport.handshake.HandshakeProtocol;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerMessageTypes;

public class StartHandoverMethod extends Methods {
private ActorRef actor;
	
	public StartHandoverMethod(ActorRef actor) {
		this.actor = actor;
	}

	@Override
	public void invoke() {
		System.out.println("START_HANDOVER GOT INVOKED");
		actor.tell(HandshakeProtocol.ServerMessageTypes.RequestStartHandover, ActorRef.noSender()); //TODO check if this is the request
	}
	
	@Override
	public String getInfo() {
		return "This method requests to start the Handover!";
	}

}