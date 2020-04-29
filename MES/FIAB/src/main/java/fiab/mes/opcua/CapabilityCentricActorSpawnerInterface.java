package fiab.mes.opcua;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.Props;
import fiab.opcua.CapabilityImplInfo;

public interface CapabilityCentricActorSpawnerInterface {

	public ActorRef createActorSpawner(ActorContext context);
			
	public static class SpawnRequest {
		CapabilityImplInfo info;
		
		public SpawnRequest(CapabilityImplInfo info) {
			this.info = info;
		}
		
		public CapabilityImplInfo getInfo() {
			return info;
		}
	}
}
