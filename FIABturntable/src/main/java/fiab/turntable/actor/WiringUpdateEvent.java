package fiab.turntable.actor;

import fiab.core.capabilities.basicmachine.events.MachineEvent;
import fiab.handshake.actor.LocalEndpointStatus;

public class WiringUpdateEvent extends MachineEvent {

    private final LocalEndpointStatus.LocalClientEndpointStatus les;

    public WiringUpdateEvent(String machineId, LocalEndpointStatus.LocalClientEndpointStatus les) {
        super(machineId, MachineEventType.UPDATED);
        this.les = les;
    }

    public LocalEndpointStatus.LocalClientEndpointStatus getLocalEndpointStatus() {
        return les;
    }
}
