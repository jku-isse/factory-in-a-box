package fiab.machine.foldingstation.events;

import fiab.core.capabilities.basicmachine.events.MachineUpdateEvent;
//TODO move to core package?
public class MachineCapabilityUpdateEvent  extends MachineUpdateEvent {
    /**
     * This class is published on the MachineLevelEventBus and is filled with the values from the OPCUA-Server
     *
     * @param machineId
     * @param parameterName
     * @param message
     */
    public MachineCapabilityUpdateEvent(String machineId, String parameterName, String message) {
        super(machineId, parameterName, message);
    }

    @Override
    public Object getValue() {
        return getMessage();
    }
}
