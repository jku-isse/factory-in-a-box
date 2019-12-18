package fiab.opcua.hardwaremock.methods;

import akka.actor.ActorRef;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerSide;

public class ResetMethod extends Methods{
	private ActorRef actor;
	
	public ResetMethod(ActorRef actor) {
		this.actor = actor;
	}
	@Override
	public void invoke() {
		System.out.println("RESET GOT INVOKED");
		actor.tell(ServerSide.Resetting, ActorRef.noSender());
	}

}
