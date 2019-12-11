package fiab.opcua.hardwaremock.methods;

import akka.actor.ActorRef;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerSide;

public class StopMethod extends Methods {
	ActorRef actor;
	
	public StopMethod(ActorRef actor) {
		this.actor = actor;
	}

	@Override
	public void invoke() {
		System.out.println("STOP GOT INVOKED");
		actor.tell(ServerSide.Stopping, ActorRef.noSender());
	}

}
