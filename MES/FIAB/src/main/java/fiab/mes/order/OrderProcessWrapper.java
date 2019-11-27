package fiab.mes.order;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ProcessCore.AbstractCapability;
import ProcessCore.ProcessStep;
import ProcessCore.impl.CapabilityInvocationImpl;
import ProcessCore.impl.ProcessImpl;
import fiab.mes.order.OrderProcess.StepStatusEnum;
import fiab.mes.order.msg.OrderProcessUpdateEvent;
import fiab.mes.restendpoint.requests.OrderStatusRequest;

public class OrderProcessWrapper {
	
	private String orderId;
	private Map<String, StepStatusEnum> stepStatus;
	private Map<String, List<String>> capabilities = new HashMap<>();

	public OrderProcessWrapper(String orderId, OrderStatusRequest.Response o) {
		this.orderId = orderId;
		this.stepStatus = parseProcessStepMap(o.getStatus().stepStatus);
	}
	
	public OrderProcessWrapper(String orderId, OrderProcessUpdateEvent o) {
		this.orderId = orderId;
		this.stepStatus = parseProcessStepMap(o.getStepsWithNewStatusAsReadOnlyMap());
	}
	
	public Map<String, StepStatusEnum> getStepStatus(){
		return stepStatus;
	}
	
	public String getOrderId() {
		return orderId;
	}
	
	public Map<String, List<String>> getCapabilities() {
		return capabilities;
	}
	
	private List<String> findCapabilities(AbstractCapability cap) {
		List<String> caps = new ArrayList<String>();
		if (/*cap.getDisplayName() == null &&*/cap.getCapabilities().size() > 0) { //not sure about checking displayName
			for (AbstractCapability c : cap.getCapabilities()) {
				caps.add(c.getDisplayName());
			}
			
		} else {
			caps.add(cap.getDisplayName());
		}
		return caps;
	}
	
	private Map<String, StepStatusEnum> parseProcessStepMap(Map<ProcessStep, StepStatusEnum> stepStatus) {
		return stepStatus.entrySet().stream()
			.collect(Collectors.toMap(
					s -> {
						if (s.getKey() instanceof ProcessImpl) {
							return "process";
						}
						else if (s.getKey() instanceof CapabilityInvocationImpl) {
							String capabilityDisplayName = ((CapabilityInvocationImpl)s.getKey()).getDisplayName();
							AbstractCapability cap = ((CapabilityInvocationImpl)s.getKey()).getInvokedCapability();
							capabilities.put(capabilityDisplayName, findCapabilities(cap));
							return capabilityDisplayName;
						} else if (s.getKey().getRole() == null) {
							return String.valueOf(s.getKey().hashCode());
						} else {
							return s.getKey().getRole().getName();
						}
					},
					Map.Entry::getValue
			));
	}
}
