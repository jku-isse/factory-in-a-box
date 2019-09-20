package shopfloor.agents.messages.order;

import java.util.Optional;
import ProcessCore.ProcessStep;

public class MappedOrderProcess extends OrderProcess {

	
	actorprocess.ActorAllocationExtension actorAllocs;	
	
	
	public MappedOrderProcess(ProcessCore.Process orderProcess, actorprocess.ActorAllocationExtension actorAllocs) {
		super(orderProcess);
		this.actorAllocs = actorAllocs;
	}
	
	public Optional<actorprocess.ActorAllocation> getActorAllocationForProcess(ProcessStep step) {
		return actorAllocs.getActorAllocationList().stream()
				.filter(aa -> aa.getAllocatedTo().equals(step))				
				.findFirst();
	}
	
	

	
	
	
}
