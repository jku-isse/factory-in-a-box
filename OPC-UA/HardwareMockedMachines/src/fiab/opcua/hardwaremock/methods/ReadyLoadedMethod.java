package fiab.opcua.hardwaremock.methods;

import akka.actor.ActorRef;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerSide;

public class ReadyLoadedMethod extends Methods {
private ActorRef actor;
	
	public ReadyLoadedMethod(ActorRef actor) {
		this.actor = actor;
	}

	@Override
	public void invoke() {
		System.out.println("READY_LOADED GOT INVOKED");
		actor.tell(ServerSide.ReadyEmpty, ActorRef.noSender());
	}

	@Override
	public String getInfo() {
		return "This method signals Ready Loaded!";
	}

}