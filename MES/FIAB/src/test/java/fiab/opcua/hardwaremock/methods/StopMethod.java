package fiab.opcua.hardwaremock.methods;

import akka.actor.ActorRef;
import fiab.mes.transport.handshake.HandshakeProtocol;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerMessageTypes;

public class StopMethod extends Methods {
	private ActorRef actor;
	
	public StopMethod(ActorRef actor) {
		this.actor = actor;
	}
	@Override
	public void invoke() {
		System.out.println("STOP GOT INVOKED");
		actor.tell(HandshakeProtocol.ServerMessageTypes.Stop, ActorRef.noSender());
	}
	@Override
	public String getInfo() {
		return "This method stops the Input Station!";
	}

}
