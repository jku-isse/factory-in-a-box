package shopfloor.agents.events;

import java.util.HashMap;
import java.util.Map;

import ProcessCore.ProcessStep;
import shopfloor.agents.messages.order.OrderProcess.ProcessChangeImpact;
import shopfloor.agents.messages.order.OrderProcess.StepStatusEnum;

public class OrderProcessUpdateEvent extends OrderBaseEvent{

	protected Map<ProcessStep, StepStatusEnum> stepsWithNewStatus = new HashMap<>();

	public OrderProcessUpdateEvent(String orderId, String eventSource) {
		super(orderId, eventSource, OrderEventType.PRODUCTION_UPDATE);
	}
	
	public OrderProcessUpdateEvent(String orderId, String eventSource, ProcessChangeImpact pci) {
		super(orderId, eventSource, OrderEventType.PRODUCTION_UPDATE);
		stepsWithNewStatus.put(pci.getRootChange(), pci.getCurrentState());
		pci.getImpact().entrySet().stream()
			.forEach(entry -> entry.getValue().stream()
								.forEach(step -> stepsWithNewStatus.put(step,entry.getKey()) )	
			);
	}
}
