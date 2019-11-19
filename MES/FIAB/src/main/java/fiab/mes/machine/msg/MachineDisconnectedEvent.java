  
package fiab.mes.machine.msg;

import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;

public class MachineDisconnectedEvent extends MachineEvent {

	protected AkkaActorBackedCoreModelAbstractActor machine;
	
	public MachineDisconnectedEvent(AkkaActorBackedCoreModelAbstractActor machine, String message) {
		super(machine.getId(), MachineEventType.DISCONNECTED, message);
	}
	
	public AkkaActorBackedCoreModelAbstractActor getMachine() {
		return machine;
	}
}