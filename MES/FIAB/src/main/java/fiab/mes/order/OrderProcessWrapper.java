package fiab.mes.order;

import java.util.Map;
import java.util.stream.Collectors;

import fiab.mes.order.OrderProcess.StepStatusEnum;
import fiab.mes.order.msg.OrderProcessUpdateEvent;
import fiab.mes.restendpoint.requests.OrderStatusRequest;

public class OrderProcessWrapper {
	
	private Map<String, StepStatusEnum> stepStatus;

	public OrderProcessWrapper(OrderStatusRequest.Response o) {
		this.stepStatus = o.getStatus().stepStatus.entrySet().stream()
			.collect(Collectors.toMap(
					s -> s.getKey().getRole() == null ? String.valueOf(s.getKey().hashCode()) : s.getKey().getRole().getName(), 
					Map.Entry::getValue
			));
	}
	
	public OrderProcessWrapper(OrderProcessUpdateEvent o) {
		this.stepStatus = o.getStepsWithNewStatus().entrySet().stream()
			.collect(Collectors.toMap(
					s -> s.getKey().getRole() == null ? String.valueOf(s.getKey().hashCode()) : s.getKey().getRole().getName(), 
					Map.Entry::getValue
			));
	}
	
	public Map<String, StepStatusEnum> getStepStatus(){
		return stepStatus;
	}
	
}
