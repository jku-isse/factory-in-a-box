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
		System.out.println("READY_EMPTY GOT INVOKED");
		actor.tell(ServerSide.ReadyEmpty, ActorRef.noSender()); //TODO serverside is not correct! MessageTypes.Ready not existing
	}
	
	@Override
	public String getInfo() {
		return "This method signals Ready Empty!";
	}

}
