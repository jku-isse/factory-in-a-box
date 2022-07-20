package fiab.plotter.plotting.message;

import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.basicmachine.events.MachineUpdateEvent;

public class PlottingStatusUpdateEvent extends MachineUpdateEvent {


    protected final BasicMachineStates status;

    public PlottingStatusUpdateEvent(String machineId, BasicMachineStates status) {
        super(machineId, OPCUABasicMachineBrowsenames.STATE_VAR_NAME, "Plotting State has been updated");
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
        return "TurningStatusUpdateEvent [status=" + status + ", machineId=" + machineId + "]";
    }

}
