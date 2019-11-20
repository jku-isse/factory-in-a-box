package fiab.mes.machine.msg;

import java.util.HashSet;
import java.util.Set;

import ProcessCore.AbstractCapability;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;

public class MachineConnectedEvent extends MachineEvent {

	protected Set<ProcessCore.AbstractCapability> providedMachineCapabilities = new HashSet<>();
	protected Set<ProcessCore.AbstractCapability> requiredMachineCapabilities = new HashSet<>();
	
	protected AkkaActorBackedCoreModelAbstractActor machine = null;
	
	public MachineConnectedEvent(AkkaActorBackedCoreModelAbstractActor machine, String message) {
		super(machine.getId(), MachineEventType.CONNECTED, message);
		this.machine = machine;
	}

	public MachineConnectedEvent(AkkaActorBackedCoreModelAbstractActor machine, Set<AbstractCapability> providedMachineCapabilities, Set<AbstractCapability> requiredMachineCapabilities, String message) {
		super(machine.getId(), MachineEventType.CONNECTED, message);
		this.machine = machine;
		this.providedMachineCapabilities = providedMachineCapabilities;
		this.requiredMachineCapabilities = requiredMachineCapabilities;
	}
	
	public MachineConnectedEvent(AkkaActorBackedCoreModelAbstractActor machine) {
		super(machine.getId(), MachineEventType.CONNECTED);
		this.machine = machine;
	}

	public MachineConnectedEvent(AkkaActorBackedCoreModelAbstractActor machine, Set<AbstractCapability> providedMachineCapabilities, Set<AbstractCapability> requiredMachineCapabilities) {
		super(machine.getId(), MachineEventType.CONNECTED);
		this.machine = machine;
		this.providedMachineCapabilities = providedMachineCapabilities;
		this.requiredMachineCapabilities = requiredMachineCapabilities;
	}

	public Set<ProcessCore.AbstractCapability> getProvidedMachineCapabilities() {
		return providedMachineCapabilities;
	}

	public Set<ProcessCore.AbstractCapability> getRequiredMachineCapabilities() {
		return requiredMachineCapabilities;
	}

	public AkkaActorBackedCoreModelAbstractActor getMachine() {
		return machine;
	}		
}