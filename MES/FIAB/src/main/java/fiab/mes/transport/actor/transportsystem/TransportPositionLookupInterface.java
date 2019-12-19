package fiab.mes.transport.actor.transportsystem;

import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface.Position;

public interface TransportPositionLookupInterface {
	public Position getPositionForActor(AkkaActorBackedCoreModelAbstractActor actor);
}