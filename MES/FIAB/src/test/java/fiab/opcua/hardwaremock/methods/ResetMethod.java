package fiab.opcua.hardwaremock.methods;

import akka.actor.ActorRef;
import fiab.core.capabilities.handshake.IOStationCapability;

public class ResetMethod extends Methods{
	private ActorRef actor;
	
	public ResetMethod(ActorRef actor) {
		this.actor = actor;
	}
	@Override
	public void invoke() {
		System.out.println("RESET GOT INVOKED");
		actor.tell(IOStationCapability.ServerMessageTypes.Reset, ActorRef.noSender());
	}
	@Override
	public String getInfo() {
		return "This method resets the Input Station!";
	}

}
