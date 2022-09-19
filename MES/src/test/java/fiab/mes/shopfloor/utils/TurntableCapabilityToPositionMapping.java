package fiab.mes.shopfloor.utils;

import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_SELF;

/**
 * Helper class to map positions to local turntable capabilities
 */
public class TurntableCapabilityToPositionMapping {

    private final Map<String, TransportRoutingInterface.Position> capabilityToPosition;
    private final Map<TransportRoutingInterface.Position, String> positionToCapability;

    public TurntableCapabilityToPositionMapping(TransportRoutingInterface.Position turntablePosition) {
        this.capabilityToPosition = new HashMap<>();
        this.positionToCapability = new HashMap<>();
        mapCapabilityToPosition(TRANSPORT_MODULE_SELF, turntablePosition);
    }

    public void mapCapabilityToPosition(String localCapability, TransportRoutingInterface.Position position) {
        this.capabilityToPosition.put(localCapability, position);
        this.positionToCapability.put(position, localCapability);
    }

    public TransportRoutingInterface.Position getPositionForCapability(String localCapability) {
        return this.capabilityToPosition.getOrDefault(localCapability, TransportRoutingInterface.UNKNOWN_POSITION);
    }

    public Optional<String> getCapabilityForPosition(TransportRoutingInterface.Position position) {
        String capability = this.positionToCapability.get(position);
        if (capability == null) return Optional.empty();
        return Optional.of(capability);
    }
}
