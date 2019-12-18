package fiab.opcua.hardwaremock.methods;

import akka.actor.ActorRef;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerSide;

public class ReadyEmptyMethod extends Methods {
private ActorRef actor;
	
	public ReadyEmptyMethod(ActorRef actor) {
		this.actor = actor;
	}

	@Override
	public void invoke() {
		System.out.println("STOP GOT INVOKED");
		actor.tell(ServerSide.ReadyEmpty, ActorRef.noSender());
	}

}
