package fiab.opcua.hardwaremock.methods;

import akka.actor.ActorRef;
import fiab.core.capabilities.handshake.IOStationCapability;
import fiab.core.capabilities.handshake.HandshakeCapability.ServerMessageTypes;

public class StartHandoverMethod extends Methods {
private ActorRef actor;
	
	public StartHandoverMethod(ActorRef actor) {
		this.actor = actor;
	}

	@Override
	public void invoke() {
		System.out.println("START_HANDOVER GOT INVOKED");
		actor.tell(IOStationCapability.ServerMessageTypes.RequestStartHandover, ActorRef.noSender()); //TODO check if this is the request
	}
	
	@Override
	public String getInfo() {
		return "This method requests to start the Handover!";
	}

}