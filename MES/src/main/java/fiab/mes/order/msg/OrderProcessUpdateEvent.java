package fiab.mes.order.msg;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import ProcessCore.ProcessStep;
import fiab.mes.order.OrderProcess.ProcessChangeImpact;
import fiab.mes.order.OrderProcess.StepStatusEnum;

public class OrderProcessUpdateEvent extends OrderEvent {

	protected Map<ProcessStep, StepStatusEnum> stepsWithNewStatus = new HashMap<>();

	public OrderProcessUpdateEvent(String orderId, String eventSource, String message) {
		super(orderId, eventSource, OrderEventType.PRODUCTION_UPDATE, message);
	}
	
	public OrderProcessUpdateEvent(String orderId, String eventSource, String message, ProcessChangeImpact pci) {
		super(orderId, eventSource, OrderEventType.PRODUCTION_UPDATE, message);
		stepsWithNewStatus.put(pci.getRootChange(), pci.getCurrentState());
		pci.getImpact().entrySet().stream()
			.forEach(entry -> entry.getValue().stream()
								.forEach(step -> stepsWithNewStatus.put(step,entry.getKey()) )	
			);
	}
	
	public Map<ProcessStep, StepStatusEnum> getStepsWithNewStatusAsReadOnlyMap() {
		return Collections.unmodifiableMap(stepsWithNewStatus);
	}
}
