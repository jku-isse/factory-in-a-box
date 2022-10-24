package fiab.mes.assembly.monitoring;

import InstanceExtensionModel.States;
import InstanceExtensionModel.StepInstanceExtension;
import PriorityExtensionModel.PriorityExtension;
import ProcessCore.ProcessStep;
import ProcessCore.XmlRoot;
import fiab.mes.order.OrderProcess;
import partprocess.PartUsage;
import partprocess.PartUsageExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DroolsUtil {

    public static ArrayList<ProcessStep> getPartUsageListForPart(XmlRoot xmlRoot, OrderProcess orderProc, String partId) {

        Object[] partUsageExtension = xmlRoot.getExtensionContent().stream().filter(e -> e instanceof PartUsageExtension)
                .flatMap(e -> ((PartUsageExtension) e).getPartUsageList().stream()).toArray();

        ArrayList<ProcessStep> stepsList = new ArrayList<ProcessStep>();
        for (Object o: partUsageExtension) {
            PartUsage partUsage= (PartUsage) o;
            if (partUsage.getUsedPart().getID().equals(partId)){
                stepsList.add(partUsage.getProcessStep());
            }
        }
        return stepsList;
    }

    //TODO filter steps and rank them in case of mutiple candidates(part usage and processes)
    //TODO check the state of the steps from the orderProc not from the xmlRoot
    public static ProcessStep getPartUSageStepCandidate(ArrayList<ProcessStep> stepsList){
        for (ProcessStep step: stepsList) {
            if (((StepInstanceExtension) (step.getExtensions().stream()
                    .filter(e -> e.getContent() instanceof StepInstanceExtension).findFirst().get().getContent()))
                    .getCurrentState().equals(States.AVAILABLE.toString())){
                return step;
            }
        }
        return null;
    }

    public static void activateStepafterPartNotif(XmlRoot xmlRoot, OrderProcess orderProc, String partId){
        ArrayList<ProcessStep> candidates = DroolsUtil.getPartUsageListForPart(xmlRoot,orderProc, partId);
        ProcessStep mostEligibleCandidate = DroolsUtil.getPartUSageStepCandidate(candidates);

        ((StepInstanceExtension) (mostEligibleCandidate.getExtensions().stream()
                .filter(e -> e.getContent() instanceof StepInstanceExtension).findFirst().get().getContent()))
                .setCurrentState(States.ACTIVE.toString());
        System.out.println("updated step state!");
        completePredecessors(xmlRoot,mostEligibleCandidate);


    }

    public static void completePredecessors(XmlRoot xmlRoot, ProcessStep  step){
        List<ProcessStep> predecessorsList = step.getExtensions().stream().map(e -> e.getContent()).filter(e -> e instanceof PriorityExtension)
                .flatMap(e -> ((PriorityExtension) e).getPreceedingSteps().stream()).collect(Collectors.toList());
        if (predecessorsList.stream().anyMatch(e -> e.getID().equals(step.getID()))){
            ((StepInstanceExtension) (step.getExtensions().stream()
                    .filter(e -> e.getContent() instanceof StepInstanceExtension).findFirst().get().getContent()))
                    .setCurrentState(States.COMPLETED.toString());
        }
    }

    //public static void availableSuccessors(XmlRoot xmlRoot, ProcessStep  step)


}
