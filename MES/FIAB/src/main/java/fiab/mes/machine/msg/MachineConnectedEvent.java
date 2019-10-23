package fiab.mes.machine.msg;

import java.util.ArrayList;
import java.util.List;

import ProcessCore.AbstractCapability;

public class MachineConnectedEvent extends MachineEvent {

	protected List<ProcessCore.AbstractCapability> machineCapabilities = new ArrayList<>();
	
	public MachineConnectedEvent(String machineId) {
		super(machineId, MachineEventType.CONNECTED);
	}

	public MachineConnectedEvent(String machineId, List<AbstractCapability> machineCapabilities) {
		super(machineId, MachineEventType.CONNECTED);
		this.machineCapabilities = machineCapabilities;
	}

	public List<ProcessCore.AbstractCapability> getMachineCapabilities() {
		return machineCapabilities;
	}

	
}