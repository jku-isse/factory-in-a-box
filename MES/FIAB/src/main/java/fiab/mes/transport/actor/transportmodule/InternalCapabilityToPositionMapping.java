package fiab.mes.transport.actor.transportmodule;

import java.util.Optional;

import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface.Position;

public interface InternalCapabilityToPositionMapping {
	public Position getPositionForCapability(String capabilityId, Position selfPos);
	public Optional<String> getCapabilityIdForPosition(Position pos, Position selfPos);
}
