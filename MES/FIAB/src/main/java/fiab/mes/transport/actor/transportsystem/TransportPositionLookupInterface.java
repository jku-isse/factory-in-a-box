package fiab.mes.transport.actor.transportsystem;

import java.util.Optional;

import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface.Position;

public interface TransportPositionLookupInterface {
	public Position getPositionForActor(AkkaActorBackedCoreModelAbstractActor actor);
	public Optional<AkkaActorBackedCoreModelAbstractActor> getActorForPosition(Position pos);
}