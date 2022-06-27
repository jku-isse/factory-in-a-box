package fiab.handshake.client.messages;

import fiab.core.capabilities.basicmachine.events.MachineUpdateEvent;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers;
import fiab.core.capabilities.wiring.WiringInfo;

public class WiringUpdateNotification extends MachineUpdateEvent {

    private final WiringInfo wiringInfo;
    /**
     * This class is published on the MachineLevelEventBus and is filled with the values from the OPCUA-Server
     *
     * @param machineId FunctionalUnit id
     * @param wiringInfo updated wiringInfo
     */
    public WiringUpdateNotification(String machineId, WiringInfo wiringInfo) {
        super(machineId, "ClientHandshakeWiring",
                "The Wiring information of " + machineId + " has been updated");
        this.wiringInfo = wiringInfo;
    }

    public WiringInfo getWiringInfo() {
        return wiringInfo;
    }

    @Override
    public Object getValue() {
        return wiringInfo;
    }
}
