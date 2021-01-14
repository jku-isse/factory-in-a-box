package main.java.fiab.core.capabilities.basicmachine.events;

import main.java.fiab.core.capabilities.BasicMachineStates;

public class MachineStatusUpdateEvent extends MachineUpdateEvent {

    protected BasicMachineStates status;

    public MachineStatusUpdateEvent(String machineId, String parameterName, String message, BasicMachineStates status) {
        super(machineId, parameterName, message);
        this.status = status;
    }

    public BasicMachineStates getStatus() {
        return status;
    }

    @Override
    public Object getValue() {
        return status;
    }

    @Override
    public String toString() {
        return "MachineStatusUpdateEvent [status=" + status + ", machineId=" + machineId + "]";
    }


}
