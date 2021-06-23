package fiab.mes.order.ecore;

import ProcessCore.AbstractCapability;
import ProcessCore.CapabilityInvocation;
import ProcessCore.ProcessCoreFactory;
import ProcessCore.XmlRoot;;
import fiab.core.capabilities.folding.WellknownFoldingCapability;
import fiab.mes.capabilities.plotting.EcoreProcessUtils;

public class ProduceFoldingProcess {

    public static ProcessCore.Process getSequentialBoxProcess(String prefix) {
        XmlRoot root = ProcessCoreFactory.eINSTANCE.createXmlRoot();
        for (int i = 0; i < 4; i++) {
            AbstractCapability cap = WellknownFoldingCapability.getFoldingShapeCapability();
            if (root != null)
                root.getCapabilities().add(cap);
        }

        CapabilityInvocation s1 = ProcessCoreFactory.eINSTANCE.createCapabilityInvocation();
        CapabilityInvocation s2 = ProcessCoreFactory.eINSTANCE.createCapabilityInvocation();
        CapabilityInvocation s3 = ProcessCoreFactory.eINSTANCE.createCapabilityInvocation();
        CapabilityInvocation s4 = ProcessCoreFactory.eINSTANCE.createCapabilityInvocation();
        s1.setID(prefix+"1");
        s2.setID(prefix+"2");
        s3.setID(prefix+"3");
        s4.setID(prefix+"4");
        s1.setDisplayName(root.getCapabilities().get(0).getDisplayName());
        s2.setDisplayName(root.getCapabilities().get(1).getDisplayName());
        s3.setDisplayName(root.getCapabilities().get(2).getDisplayName());
        s4.setDisplayName(root.getCapabilities().get(3).getDisplayName());
        s1.setInvokedCapability(root.getCapabilities().get(0));
        s2.setInvokedCapability(root.getCapabilities().get(1));
        s3.setInvokedCapability(root.getCapabilities().get(2));
        s4.setInvokedCapability(root.getCapabilities().get(3));
        s1.getInputMappings().add(EcoreProcessUtils.getVariableMapping(root.getCapabilities().get(0).getInputs().get(0)));
        s2.getInputMappings().add(EcoreProcessUtils.getVariableMapping(root.getCapabilities().get(1).getInputs().get(0)));
        s3.getInputMappings().add(EcoreProcessUtils.getVariableMapping(root.getCapabilities().get(2).getInputs().get(0)));
        s4.getInputMappings().add(EcoreProcessUtils.getVariableMapping(root.getCapabilities().get(3).getInputs().get(0)));

        ProcessCore.Process p = ProcessCoreFactory.eINSTANCE.createProcess();
        p.setDisplayName("ProcessTemplate4Folds");
        p.setID("ProcessTemplate4Folds");
        EcoreProcessUtils.addProcessvariables(p, "Box", "Box", "Box", "Box");
        EcoreProcessUtils.mapCapInputToProcessVar(p.getVariables(), s1,s2,s3,s4);

        p.getSteps().add(s1);
        p.getSteps().add(s2);
        p.getSteps().add(s3);
        p.getSteps().add(s4);
        return p;
    }
}
