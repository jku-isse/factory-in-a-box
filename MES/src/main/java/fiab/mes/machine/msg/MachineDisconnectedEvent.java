  
package fiab.mes.machine.msg;

import fiab.core.capabilities.basicmachine.events.MachineEvent;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;

public class MachineDisconnectedEvent extends MachineEvent {

	protected AkkaActorBackedCoreModelAbstractActor machine;
	
	public MachineDisconnectedEvent(AkkaActorBackedCoreModelAbstractActor machine, String message) {
		super(machine.getId(), MachineEventType.DISCONNECTED, message);
		this.machine = machine;
	}
	
	public MachineDisconnectedEvent(AkkaActorBackedCoreModelAbstractActor machine) {
		super(machine.getId(), MachineEventType.DISCONNECTED);
		this.machine = machine;
	}
	
	public MachineDisconnectedEvent(String id) {
		super(id, MachineEventType.DISCONNECTED);
	}
	
	public AkkaActorBackedCoreModelAbstractActor getMachine() {
		return machine;
	}
}