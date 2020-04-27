package fiab.opcua.hardwaremock.methods;

import akka.actor.ActorRef;
import fiab.core.capabilities.handshake.IOStationCapability;

public class CompleteMethod extends Methods {
private ActorRef actor;
	
	public CompleteMethod(ActorRef actor) {
		this.actor = actor;
	}

	@Override
	public void invoke() {
		System.out.println("COMPLETE GOT INVOKED");
		actor.tell(IOStationCapability.ServerMessageTypes.Complete, ActorRef.noSender());
	}
	
	@Override
	public String getInfo() {
		return "This method sets the State to Complete!";
	}

}