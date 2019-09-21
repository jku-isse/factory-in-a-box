package fiab.mes.order;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EObject;

import com.google.common.collect.Lists;

import ProcessCore.ParallelBranches;
import ProcessCore.ProcessStep;

public class OrderProcess {
	
	protected ProcessCore.Process orderProcess;
	protected HashMap<ProcessStep, StepStatusEnum> stepStatus = new HashMap<>();
	
	public OrderProcess(ProcessCore.Process orderProcess) {
		assert orderProcess != null : "OrderProcess may not be instantiated with null";		
		this.orderProcess = orderProcess;
		initStepStates(this.orderProcess);
	}
	
	private void initStepStates(ProcessStep step) {
		stepStatus.put(step, StepStatusEnum.INITIATED);
		if (step instanceof ProcessCore.Process)
			((ProcessCore.Process) step).getSteps().forEach(childStep -> initStepStates(childStep));
		if (step instanceof ParallelBranches) {
			((ParallelBranches)step).getBranches().forEach(childStep -> initStepStates(childStep));
		}
	}
	
	public ProcessChangeImpact activateProcess() {		
		StepStatusEnum prev = stepStatus.put(orderProcess, StepStatusEnum.AVAILABLE);
		ProcessChangeImpact pci = new ProcessChangeImpact(orderProcess, prev, StepStatusEnum.AVAILABLE);
		orderProcess.getSteps().stream().findFirst().ifPresent(childStep -> makeNextStepsAvailable(childStep, pci));
		return pci;
	}
	
	private void makeNextStepsAvailable(ProcessStep step, ProcessChangeImpact pci) {
		// makes step available if in state initiated, then also recursively continue, and only if prior ones are completed or canceled
		if (stepStatus.get(step).equals(StepStatusEnum.INITIATED)) {
			stepStatus.put(step, StepStatusEnum.AVAILABLE);
			pci.markStepInNewState(StepStatusEnum.AVAILABLE, step);
		}
		if (step instanceof ProcessCore.Process)
			// check if one step is active or available then nothing new to make available, else find first in state initiated
			if (((ProcessCore.Process) step).getSteps().stream()
					.anyMatch(child -> { StepStatusEnum status = stepStatus.getOrDefault(child, StepStatusEnum.INITIATED);
									return (status.equals(StepStatusEnum.AVAILABLE) || status.equals(StepStatusEnum.ACTIVE)); } )) {
				return;
			} else {
				((ProcessCore.Process) step).getSteps().stream()
					.filter(child -> stepStatus.getOrDefault(child, StepStatusEnum.INITIATED).equals(StepStatusEnum.INITIATED))
					.findFirst().ifPresent(childStep -> makeNextStepsAvailable(childStep, pci));
			}
		if (step instanceof ParallelBranches) {
			((ParallelBranches)step).getBranches().forEach(childStep -> makeNextStepsAvailable(childStep, pci));
		}		
	}
	
	public ProcessChangeImpact activateStepIfAllowed(ProcessStep step) {
		if (isStepReadyForProduction(step)) {
			StepStatusEnum prev = stepStatus.put(step, StepStatusEnum.ACTIVE);
			return new ProcessChangeImpact(step, prev, StepStatusEnum.ACTIVE);
		} else {
			StepStatusEnum prev = stepStatus.getOrDefault(step, StepStatusEnum.INITIATED);
			return new ProcessChangeImpact(step, prev, prev);
		}
	}
	
	public ProcessChangeImpact markStepComplete(ProcessStep step) {
		ProcessChangeImpact pci = new ProcessChangeImpact(step, stepStatus.getOrDefault(step, StepStatusEnum.INITIATED), StepStatusEnum.COMPLETED);
		propagateStepComplete(step, pci);
		return pci;
	}
	
	
	// returns previous status
	private void propagateStepComplete(ProcessStep step, final ProcessChangeImpact pci) {
		StepStatusEnum prevStatus = stepStatus.put(step, StepStatusEnum.COMPLETED);		
		pci.markStepInNewState(StepStatusEnum.COMPLETED, step);
		if (prevStatus.equals(StepStatusEnum.ACTIVE)) {
			// mark all subtasks as complete,
			if (step instanceof ProcessCore.Process)
				((ProcessCore.Process) step).getSteps().stream().forEach(childStep -> propagateStepComplete(childStep, pci));
			if (step instanceof ParallelBranches) {
				((ParallelBranches)step).getBranches().forEach(childStep -> propagateStepComplete(childStep, pci));
			}
			// activate next tasks in sequence somewhere 
			propagateCompletionToParent(step, pci);
		}
	}		
	
	private void propagateCompletionToParent(ProcessStep step, ProcessChangeImpact pci) {
		EObject parentAsO = step.eContainer();
		if (parentAsO != null && parentAsO instanceof ProcessStep) {			
			ProcessStep parent = (ProcessStep)parentAsO;
			if (areAllChildrenOfProcessCancelledOrCompleted(parent)) {
				// if all child steps complete or canceled propagate up
				stepStatus.put(parent, StepStatusEnum.COMPLETED);	
				pci.markStepInNewState(StepStatusEnum.COMPLETED, parent);
				propagateCompletionToParent(parent, pci);
			} else { // not all substeps are complete or canceled
				makeNextStepsAvailable(parent, pci);
			}
		}
	}

	public boolean areAllTasksCancelledOrCompleted() {
		return areAllChildrenOfProcessCancelledOrCompleted(orderProcess);
	}
	
	// returns true if there are no children
	private boolean areAllChildrenOfProcessCancelledOrCompleted(ProcessStep step) {
		if (step instanceof ProcessCore.Process)
			return ((ProcessCore.Process) step).getSteps().stream().allMatch(childStep -> {
					StepStatusEnum status = stepStatus.getOrDefault(childStep, StepStatusEnum.INITIATED);
					return status.equals(StepStatusEnum.COMPLETED) || status.equals(StepStatusEnum.CANCELED);	
				} );
		if (step instanceof ParallelBranches) {
			return ((ParallelBranches)step).getBranches().stream().allMatch(childStep -> { 
					StepStatusEnum status = stepStatus.getOrDefault(childStep, StepStatusEnum.INITIATED);
					return status.equals(StepStatusEnum.COMPLETED) || status.equals(StepStatusEnum.CANCELED);	
				} );
		} 
		return true; 
	}
	
	public ProcessChangeImpact markStepCanceled(ProcessStep step) {
		ProcessChangeImpact pci = new ProcessChangeImpact(step, stepStatus.getOrDefault(step, StepStatusEnum.INITIATED), StepStatusEnum.CANCELED);
		propagateStepCanceled(step, pci);
		return pci;
	}
	
	// returns previous status
	private void propagateStepCanceled(ProcessStep step, final ProcessChangeImpact pci) {
		StepStatusEnum prevStatus = stepStatus.getOrDefault(step, StepStatusEnum.INITIATED);			
		if (!prevStatus.equals(StepStatusEnum.COMPLETED) && !prevStatus.equals(StepStatusEnum.CANCELED) ) {
			stepStatus.put(step, StepStatusEnum.CANCELED);
			pci.markStepInNewState(StepStatusEnum.CANCELED, step);
			// mark all subtasks as canceled,
			if (step instanceof ProcessCore.Process)
				((ProcessCore.Process) step).getSteps().stream().forEach(childStep -> propagateStepCanceled(childStep, pci));
			if (step instanceof ParallelBranches) {
				((ParallelBranches)step).getBranches().forEach(childStep -> propagateStepCanceled(childStep, pci));
			}
			// activate next tasks in sequence somewhere 
			propagateCompletionToParent(step, pci);
		}
	}
	
	public List<ProcessStep> getAvailableSteps() {			
		return getReadyStepsForProduction(orderProcess);				
	}
	
	private List<ProcessStep> getReadyStepsForProduction(ProcessStep step) {
		if (step instanceof ProcessCore.Process)
			return getReadyStepsFromSequence((ProcessCore.Process) step);
		if (step instanceof ParallelBranches) {
			return ((ParallelBranches) step).getBranches().stream()
					.flatMap(paraStep -> getReadyStepsForProduction(paraStep).stream())
					.collect(Collectors.toList());
		}
		if (isStepReadyForProduction(step)) {
			return Lists.newArrayList(step);
		} else return Collections.emptyList();
	}
	
	private List<ProcessStep> getReadyStepsFromSequence(ProcessCore.Process seq) {
		// we need to mark steps as available, active, complete etc
		return seq.getSteps().stream()
			.filter(step -> isStepReadyForProduction(step))
			.flatMap(childStep -> getReadyStepsForProduction(childStep).stream())
			.collect(Collectors.toList());
	}
	
	private boolean isStepReadyForProduction(ProcessStep step) {
		return stepStatus.getOrDefault(step, StepStatusEnum.INITIATED).equals(StepStatusEnum.AVAILABLE);		
	}
	
	public static enum StepStatusEnum { INITIATED, AVAILABLE, ACTIVE, CANCELED, HALTED, COMPLETED }
	
	public static class ProcessChangeImpact {
		protected ProcessStep rootChange;
		protected StepStatusEnum prevState;
		protected StepStatusEnum currentState;
		protected HashMap<StepStatusEnum,Set<ProcessStep>> impact = new HashMap<>();
		
		public ProcessChangeImpact(ProcessStep rootChange, StepStatusEnum prevState, StepStatusEnum currentState) {
			super();
			this.rootChange = rootChange;
			this.prevState = prevState;
			this.currentState = currentState;
		}					
				
		public ProcessStep getRootChange() {
			return rootChange;
		}

		public StepStatusEnum getPrevState() {
			return prevState;
		}

		public StepStatusEnum getCurrentState() {
			return currentState;
		}

		public HashMap<StepStatusEnum, Set<ProcessStep>> getImpact() {
			return impact;
		}

		public void markStepInNewState(StepStatusEnum newState, ProcessStep step) {
			impact.computeIfAbsent(newState, k ->new HashSet<ProcessStep>()).add(step);
		}
	}
	
}
