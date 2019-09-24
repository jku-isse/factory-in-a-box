package shopfloor.infra;

import java.util.HashMap;
import java.util.Map;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import shopfloor.infra.AbstractOpcuaActorPropsFactory.OpcuaEntryNodeInfo;
import shopfloor.infra.AbstractOpcuaActorPropsFactory.RegisterActorDefForCapability;

public class ShopfloorScanner extends AbstractActor {

	private Map<String, RegisterActorDefForCapability> typeDefMap = new HashMap<>();

	static public Props props() {
		return Props.create(ShopfloorScanner.class, () -> new ShopfloorScanner());
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
		        .match(RegisterActorDefForCapability.class, msg -> {
		        	typeDefMap.put(msg.capabilityNodeName, msg);
		        })
		        .build();
	}

	//create actors for all capabilities found
	private void instantiateActor(AbstractOpcuaActorPropsFactory fact, OpcuaEntryNodeInfo entryPoint) {
		ActorRef actor = getContext().actorOf(fact.getProps(entryPoint));
	}
	
	// notify about actor/machine/participant creation
	
	
}
