package fiab.mes.order;

import java.util.Map;
import java.util.stream.Collectors;

import fiab.mes.order.OrderProcess.StepStatusEnum;
import fiab.mes.order.msg.OrderProcessUpdateEvent;
import fiab.mes.restendpoint.requests.OrderStatusRequest;

public class OrderProcessWrapper {
	
	private String orderId;
	private Map<String, StepStatusEnum> stepStatus;

	public OrderProcessWrapper(String orderId, OrderStatusRequest.Response o) {
		this.orderId = orderId;
		this.stepStatus = o.getStatus().stepStatus.entrySet().stream()
			.collect(Collectors.toMap(
					s -> s.getKey().getRole() == null ? String.valueOf(s.getKey().hashCode()) : s.getKey().getRole().getName(), 
					Map.Entry::getValue
			));
	}
	
	public OrderProcessWrapper(OrderProcessUpdateEvent o) {
		this.stepStatus = o.getStepsWithNewStatusAsReadOnlyMap().entrySet().stream()
			.collect(Collectors.toMap(
					s -> s.getKey().getRole() == null ? String.valueOf(s.getKey().hashCode()) : s.getKey().getRole().getName(), 
					Map.Entry::getValue
			));
	}
	
	public Map<String, StepStatusEnum> getStepStatus(){
		return stepStatus;
	}
	
	public String getOrderId() {
		return orderId;
	}
	
}
