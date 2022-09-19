package fiab.mes.shopfloor.participants;

import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Used in testing to map participants (machines/humans) to a position on the shopfloor
 */
public class PositionMap extends HashMap<String, ParticipantInfo> {

    /**
     * Stores the position inside a participantInfo object, which selects a free port we can use in testing
     *
     * @param machineId unique id
     * @param position  the position on the shopfloor
     */
    public void addPositionMapping(String machineId, TransportRoutingInterface.Position position) {
        put(machineId, new ParticipantInfo(machineId, position));
    }

    public TransportRoutingInterface.Position getPositionForId(String machineId) {
        ParticipantInfo info = get(machineId);
        if (info == null) return TransportRoutingInterface.UNKNOWN_POSITION;
        return info.getPosition();
    }

    public Optional<String> getMachineIdForPosition(TransportRoutingInterface.Position position) {
        String machineId = null;
        for (String id : keySet()) {
            if (get(id).getPosition().equals(position)) machineId = id;
        }
        if (machineId == null) return Optional.empty();
        return Optional.of(machineId);
    }

    public int getOpcUaPortForId(String machineId) {
        return get(machineId).getOpcUaPort();
    }
}
