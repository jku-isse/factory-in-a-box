package fiab.mes.order;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ProcessCore.AbstractCapability;
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
		this.stepStatus = o.getStatus().stepStatus.entrySet().stream()
			.collect(Collectors.toMap(
					s -> {
						if (s.getKey() instanceof ProcessImpl) {
							return "process";
						}
						else if (s.getKey() instanceof CapabilityInvocationImpl) {
							String capabilityDisplayName = ((CapabilityInvocationImpl)s.getKey()).getDisplayName();
							AbstractCapability cap = ((CapabilityInvocationImpl)s.getKey()).getInvokedCapability();
							List<String> caps = new ArrayList<String>();
							for (AbstractCapability c : cap.getCapabilities()) {
								caps.add(c.getDisplayName());
							}
							capabilities.put(capabilityDisplayName, caps);
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
	
	public OrderProcessWrapper(String orderId, OrderProcessUpdateEvent o) {
		this.orderId = orderId;
		this.stepStatus = o.getStepsWithNewStatusAsReadOnlyMap().entrySet().stream()
			.collect(Collectors.toMap(
					s -> {
						if (s.getKey() instanceof ProcessImpl) {
							return "process";
						}
						else if (s.getKey() instanceof CapabilityInvocationImpl) {
							String capabilityDisplayName = ((CapabilityInvocationImpl)s.getKey()).getDisplayName();
							AbstractCapability cap = ((CapabilityInvocationImpl)s.getKey()).getInvokedCapability();
							List<String> caps = new ArrayList<String>();
							for (AbstractCapability c : cap.getCapabilities()) {
								caps.add(c.getDisplayName());
							}
							System.out.println(caps);
							capabilities.put(capabilityDisplayName, caps);
							return capabilityDisplayName;
						}
						else if (s.getKey().getRole() == null) {
							return String.valueOf(s.getKey().hashCode());
						} else {
							return s.getKey().getRole().getName();
						}
					},
					Map.Entry::getValue
			));
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
	
}
