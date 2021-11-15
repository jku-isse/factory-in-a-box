package fiab.mes;

import akka.actor.ActorSystem;

public interface ShopfloorDiscovery {
	void triggerDiscoveryMechanism(ActorSystem system);
}
