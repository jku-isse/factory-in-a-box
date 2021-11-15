package fiab.mes.order;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ProcessCore.AbstractCapability;
import ProcessCore.ProcessStep;
import ProcessCore.impl.CapabilityInvocationImpl;
import ProcessCore.impl.ParallelBranchesImpl;
import ProcessCore.impl.ProcessImpl;
import fiab.mes.order.OrderProcess.StepStatusEnum;
import fiab.mes.order.msg.OrderProcessUpdateEvent;
import fiab.mes.restendpoint.requests.OrderStatusRequest;

public class OrderProcessWrapper {
	
	private String orderId;
	private Map<String, StepStatusEnum> stepStatus = new HashMap<>();
	private Map<String, List<String>> capabilities = new HashMap<>();
		
	private OrderProcessWrapper(String orderId, Map<ProcessStep, StepStatusEnum> o) {
		this.orderId = orderId;
		fillMaps(o);
	}

	public OrderProcessWrapper(String orderId, OrderStatusRequest.Response o) {
		this(orderId, o.getStatus().stepStatus);
		reorderMaps(o.getStatus().orderProcess);
	}
	
	public OrderProcessWrapper(String orderId, OrderProcessUpdateEvent o) {
		this(orderId, o.getStepsWithNewStatusAsReadOnlyMap());
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
	
	private void fillMaps(Map<ProcessStep, StepStatusEnum> stepStatus) {
		this.stepStatus = stepStatus.entrySet().stream()
			.filter(s -> !(s.getKey() instanceof ParallelBranchesImpl))
			.filter(s -> s.getKey() instanceof ProcessImpl || s.getKey() instanceof CapabilityInvocationImpl)
			.collect(Collectors.toMap(
					s -> {
						String frontendId = generateId(s.getKey());
						if (s.getKey() instanceof CapabilityInvocationImpl) {
							AbstractCapability cap = ((CapabilityInvocationImpl)s.getKey()).getInvokedCapability();
							capabilities.put(frontendId, findCapabilities(cap));
						}
						return frontendId;
					},
					Map.Entry::getValue
			));
	}
	
	private String generateId(ProcessStep ps) {
		String id = ps.getID() == null ? "" : ps.getID();
		String name = ps.getDisplayName() == null ? "" : ps.getDisplayName();
		return id + " " + name;
	}
	
	private void reorderMaps(ProcessCore.Process p) {
		stepStatus = reorderHashMapByProcess(stepStatus, p);
		capabilities = reorderHashMapByProcess(capabilities, p);
	}
	
	private <T> Map<String, T> reorderHashMapByProcess(Map<String, T> hashMap, ProcessCore.Process p) {
		Map<String, T> linkedHashMap = new LinkedHashMap<>();
		if (p.getSteps().size() == 1 && p.getSteps().get(0) instanceof ParallelBranchesImpl) {
			((ParallelBranchesImpl)p.getSteps().get(0)).getBranches().stream().forEach(b -> {
				linkedHashMap.put(generateId(b), hashMap.get(generateId(b)));
				b.getSteps().stream().forEach(s -> linkedHashMap.put(generateId(s), hashMap.get(generateId(s))));
			});
		} else {
			linkedHashMap.put(generateId(p), hashMap.get(generateId(p)));
			p.getSteps().stream().forEach(s -> linkedHashMap.put(generateId(s), hashMap.get(generateId(s))));
		}
		return linkedHashMap;
	}
}
