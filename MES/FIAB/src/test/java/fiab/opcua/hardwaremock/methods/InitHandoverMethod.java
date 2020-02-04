package fiab.opcua.hardwaremock.methods;

import akka.actor.ActorRef;
import fiab.mes.mockactors.MockServerHandshakeActor.MessageTypes;

public class InitHandoverMethod extends Methods {
private ActorRef actor;
	
	public InitHandoverMethod(ActorRef actor) {
		this.actor = actor;
	}

	@Override
	public void invoke() {
		System.out.println("INIT_HANDOVER GOT INVOKED");
		actor.tell(MessageTypes.RequestInitiateHandover, ActorRef.noSender()); //TODO check if this is the request
	}
	
	@Override
	public String getInfo() {
		return "This method initializes the Handover!";
	}

}