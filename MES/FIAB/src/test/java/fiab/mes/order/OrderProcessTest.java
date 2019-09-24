package fiab.mes.order;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.Before;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ProcessCore.AbstractCapability;
import ProcessCore.CapabilityInvocation;
import ProcessCore.ParallelBranches;
import ProcessCore.ProcessCoreFactory;
import ProcessCore.ProcessStep;
import fiab.mes.order.OrderProcess.ProcessChangeImpact;
import fiab.mes.order.OrderProcess.StepStatusEnum;

class OrderProcessTest {

	OrderProcess testOrder;
	public CapabilityInvocation s1 = ProcessCoreFactory.eINSTANCE.createCapabilityInvocation();
	public CapabilityInvocation s2 = ProcessCoreFactory.eINSTANCE.createCapabilityInvocation();
	public CapabilityInvocation s3 = ProcessCoreFactory.eINSTANCE.createCapabilityInvocation();
	public CapabilityInvocation s4 = ProcessCoreFactory.eINSTANCE.createCapabilityInvocation();
	
	@BeforeEach
	void setUp() throws Exception {
		setupSteps();
	}
	
	@Test
	void testActivateSequentialProcess() { 
		testOrder = new OrderProcess(getSequentialProcess());
		ProcessChangeImpact pci1 = testOrder.activateProcess();
		List<ProcessStep> steps = testOrder.getAvailableSteps();
		assert(!steps.isEmpty());
		assert(steps.get(0).equals(s1));
	}

	@Test
	void testActivateParallelProcess() { 
		testOrder = new OrderProcess(getParallelProcess());
		ProcessChangeImpact pci1 = testOrder.activateProcess();
		List<ProcessStep> steps = testOrder.getAvailableSteps();
		assert(steps.size() == 4);
		assert(steps.get(0).equals(s1));
	}
	
	@Test
	void testActivateStepIfAllowedOnSeqentialProcess() {
		testOrder = new OrderProcess(getSequentialProcess());
		ProcessChangeImpact pci1 = testOrder.activateProcess();
		List<ProcessStep> steps = testOrder.getAvailableSteps();
		ProcessChangeImpact pci2 = testOrder.activateStepIfAllowed(s1);
		assert(pci2.currentState.equals(StepStatusEnum.ACTIVE));
		assert(pci2.prevState.equals(StepStatusEnum.AVAILABLE));
	}

	@Test
	void testWrongActivateStepIfAllowedOnSeqentialProcess() {
		testOrder = new OrderProcess(getSequentialProcess());
		ProcessChangeImpact pci1 = testOrder.activateProcess();
		ProcessChangeImpact pci2 = testOrder.activateStepIfAllowed(s2);
		assert(pci2.currentState.equals(StepStatusEnum.INITIATED));
		assert(pci2.prevState.equals(StepStatusEnum.INITIATED));
	}
	
	@Test
	void testAnyActivateStepIfAllowedOnParallelProcess() {
		testOrder = new OrderProcess(getParallelProcess());
		ProcessChangeImpact pci1 = testOrder.activateProcess();
		ProcessChangeImpact pci2 = testOrder.activateStepIfAllowed(s2);
		assert(pci2.currentState.equals(StepStatusEnum.ACTIVE));
		assert(pci2.prevState.equals(StepStatusEnum.AVAILABLE));
	}
	
	
	@Test
	void testMarkStepCompleteOnSequentialProcess() {
		testOrder = new OrderProcess(getSequentialProcess());
		ProcessChangeImpact pci1 = testOrder.activateProcess();
		List<ProcessStep> steps = testOrder.getAvailableSteps();
		ProcessChangeImpact pci2 = testOrder.activateStepIfAllowed(s1);
		ProcessChangeImpact pci3 = testOrder.markStepComplete(s1);
		assert(pci3.currentState.equals(StepStatusEnum.COMPLETED));
		assert(pci3.prevState.equals(StepStatusEnum.ACTIVE));
		steps = testOrder.getAvailableSteps();
		ProcessChangeImpact pci4 = testOrder.activateStepIfAllowed(s2);
		assert(pci4.currentState.equals(StepStatusEnum.ACTIVE));
		assert(pci4.prevState.equals(StepStatusEnum.AVAILABLE));
	}
	
	@Test
	void testMarkStepCompleteOnParallelProcess() {
		testOrder = new OrderProcess(getParallelProcess());
		ProcessChangeImpact pci1 = testOrder.activateProcess();
		List<ProcessStep> steps = testOrder.getAvailableSteps();
		ProcessChangeImpact pci2 = testOrder.activateStepIfAllowed(s3);
		ProcessChangeImpact pci3 = testOrder.markStepComplete(s3);
		assert(pci3.currentState.equals(StepStatusEnum.COMPLETED));
		assert(pci3.prevState.equals(StepStatusEnum.ACTIVE));
	}

	@Test
	void testAreAllTasksCancelledOrCompleted() {
		testOrder = new OrderProcess(getSequentialProcess());
		ProcessChangeImpact pci1 = testOrder.activateProcess();
		ProcessChangeImpact pci2 = testOrder.activateStepIfAllowed(s1);
		ProcessChangeImpact pci3 = testOrder.markStepComplete(s1);
		ProcessChangeImpact pci4 = testOrder.markStepCanceled(s2);
		ProcessChangeImpact pci5 = testOrder.markStepCanceled(s3);
		ProcessChangeImpact pci6 = testOrder.markStepComplete(s4);
		assert(testOrder.areAllTasksCancelledOrCompleted());
	}
	
	@Test
	void testAreAllTasksCancelledAtOnceInParallelProcess() {
		testOrder = new OrderProcess(getParallelProcess());
		ProcessChangeImpact pci1 = testOrder.activateProcess();
		testOrder.markStepCanceled(testOrder.orderProcess);
		assert(testOrder.areAllTasksCancelledOrCompleted());
	}
	
	@Test
	void testAreAllTasksCompletedAtOnceInSequentialProcess() {
		testOrder = new OrderProcess(getSequentialProcess());
		ProcessChangeImpact pci1 = testOrder.activateProcess();
		testOrder.markStepComplete(testOrder.orderProcess);
		assert(!testOrder.areAllTasksCancelledOrCompleted());
	}

	@Test
	void testAreAllTasksCancelledAtOnceInSequentialProcess() {
		testOrder = new OrderProcess(getSequentialProcess());
		ProcessChangeImpact pci1 = testOrder.activateProcess();
		testOrder.markStepCanceled(testOrder.orderProcess);
		assert(testOrder.areAllTasksCancelledOrCompleted());
	}
	
	@Test
	void testMarkStepCanceled() {
		testOrder = new OrderProcess(getSequentialProcess());
		ProcessChangeImpact pci1 = testOrder.activateProcess();
		ProcessChangeImpact pci2 = testOrder.activateStepIfAllowed(s1);
		ProcessChangeImpact pci3 = testOrder.markStepComplete(s1);
		ProcessChangeImpact pci4 = testOrder.markStepCanceled(s2);
		assert(pci4.prevState.equals(StepStatusEnum.AVAILABLE));
		assert(pci4.currentState.equals(StepStatusEnum.CANCELED));
	}
	
	private ProcessCore.Process getSequentialProcess() {
		ProcessCore.Process p = ProcessCoreFactory.eINSTANCE.createProcess();
		p.getSteps().add(s1);
		p.getSteps().add(s2);
		p.getSteps().add(s3);
		p.getSteps().add(s4);
		return p;
	}
	
	public ProcessCore.Process getParallelProcess() {
		ProcessCore.Process p = ProcessCoreFactory.eINSTANCE.createProcess();
		ParallelBranches paraB = ProcessCoreFactory.eINSTANCE.createParallelBranches();
		p.getSteps().add(paraB);
		
		ProcessCore.Process paraP1 = ProcessCoreFactory.eINSTANCE.createProcess();
		paraP1.getSteps().add(s1);
		paraB.getBranches().add(paraP1);
		ProcessCore.Process paraP2 = ProcessCoreFactory.eINSTANCE.createProcess();
		paraP2.getSteps().add(s2);
		paraB.getBranches().add(paraP2);
		ProcessCore.Process paraP3 = ProcessCoreFactory.eINSTANCE.createProcess();
		paraP3.getSteps().add(s3);
		paraB.getBranches().add(paraP3);
		ProcessCore.Process paraP4 = ProcessCoreFactory.eINSTANCE.createProcess();
		paraP4.getSteps().add(s4);
		paraB.getBranches().add(paraP4);
		return p;
	}
	
	public void setupSteps() {
		AbstractCapability ac1 = ProcessCoreFactory.eINSTANCE.createAbstractCapability();
		ac1.setLabel("Red");
		s1.setInvocedCapability(ac1);
		AbstractCapability ac2 = ProcessCoreFactory.eINSTANCE.createAbstractCapability();
		ac2.setLabel("Blue");
		s2.setInvocedCapability(ac2);
		AbstractCapability ac3 = ProcessCoreFactory.eINSTANCE.createAbstractCapability();
		ac3.setLabel("Green");
		s3.setInvocedCapability(ac3);
		AbstractCapability ac4 = ProcessCoreFactory.eINSTANCE.createAbstractCapability();
		ac4.setLabel("Yellow");
		s4.setInvocedCapability(ac4);
	}
	
}
