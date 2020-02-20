package fiab.opcua.hardwaremock.methods;

import akka.actor.ActorRef;
import fiab.mes.transport.handshake.HandshakeProtocol;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerMessageTypes;

public class CompleteMethod extends Methods {
private ActorRef actor;
	
	public CompleteMethod(ActorRef actor) {
		this.actor = actor;
	}

	@Override
	public void invoke() {
		System.out.println("COMPLETE GOT INVOKED");
		actor.tell(HandshakeProtocol.ServerMessageTypes.Complete, ActorRef.noSender());
	}
	
	@Override
	public String getInfo() {
		return "This method sets the State to Complete!";
	}

}