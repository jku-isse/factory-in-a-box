package fiab.mes.order.ecore;

import ProcessCore.AbstractCapability;
import ProcessCore.CapabilityInvocation;
import ProcessCore.ProcessCoreFactory;
import ProcessCore.XmlRoot;;
import fiab.core.capabilities.folding.WellknownFoldingCapability;
import fiab.core.capabilities.plotting.WellknownPlotterCapability;
import fiab.mes.capabilities.plotting.EcoreProcessUtils;

public class ProduceFoldingProcess {

    public static ProcessCore.Process getSequentialBoxProcess(String prefix) {
        XmlRoot root = ProcessCoreFactory.eINSTANCE.createXmlRoot();
//        for (int i = 0; i < 4; i++) {
        AbstractCapability cap = WellknownFoldingCapability.getFoldingShapeCapability();
        if (root != null)
            root.getCapabilities().add(cap);
//        }

        CapabilityInvocation s1 = ProcessCoreFactory.eINSTANCE.createCapabilityInvocation();
//        CapabilityInvocation s2 = ProcessCoreFactory.eINSTANCE.createCapabilityInvocation();
        s1.setID(prefix + "1");
//        s2.setID(prefix+"2");
        s1.setDisplayName(root.getCapabilities().get(0).getDisplayName());
//        s2.setDisplayName(root.getCapabilities().get(1).getDisplayName());
        s1.setInvokedCapability(root.getCapabilities().get(0));
//        s2.setInvokedCapability(root.getCapabilities().get(1));
        s1.getInputMappings().add(EcoreProcessUtils.getVariableMapping(root.getCapabilities().get(0).getInputs().get(0)));
//        s2.getInputMappings().add(EcoreProcessUtils.getVariableMapping(root.getCapabilities().get(1).getInputs().get(0)));

        ProcessCore.Process p = ProcessCoreFactory.eINSTANCE.createProcess();
        p.setDisplayName("ProcessTemplate1Fold");
        p.setID("ProcessTemplate1Fold");
        EcoreProcessUtils.addProcessvariables(p, "Box"/*, "Box"*/);
        EcoreProcessUtils.mapCapInputToProcessVar(p.getVariables(), s1/*,s2,*/);

        p.getSteps().add(s1);
//        p.getSteps().add(s2);
        return p;
    }

    public static ProcessCore.Process getSequentialDrawAndFoldBoxProcess(String prefix) {
        XmlRoot root = ProcessCoreFactory.eINSTANCE.createXmlRoot();
//        for (int i = 0; i < 2; i++) {
        AbstractCapability plotCap = WellknownPlotterCapability.getColorPlottingCapability(WellknownPlotterCapability.SupportedColors.BLACK);
        AbstractCapability foldCap = WellknownFoldingCapability.getFoldingShapeCapability();
        if (root != null) {
            root.getCapabilities().add(plotCap);
            root.getCapabilities().add(foldCap);

//        }

            CapabilityInvocation s1 = ProcessCoreFactory.eINSTANCE.createCapabilityInvocation();
            CapabilityInvocation s2 = ProcessCoreFactory.eINSTANCE.createCapabilityInvocation();
            s1.setID(prefix + "1");
            s2.setID(prefix + "2");
            s1.setDisplayName(root.getCapabilities().get(0).getDisplayName());
            s2.setDisplayName(root.getCapabilities().get(1).getDisplayName());
            s1.setInvokedCapability(root.getCapabilities().get(0));
            s2.setInvokedCapability(root.getCapabilities().get(1));
            s1.getInputMappings().add(EcoreProcessUtils.getVariableMapping(root.getCapabilities().get(0).getInputs().get(0)));
            s2.getInputMappings().add(EcoreProcessUtils.getVariableMapping(root.getCapabilities().get(1).getInputs().get(0)));

            ProcessCore.Process p = ProcessCoreFactory.eINSTANCE.createProcess();
            p.setDisplayName("ProcessTemplate1PlotAndFold");
            p.setID("ProcessTemplate1PlotAndFold");
            EcoreProcessUtils.addProcessvariables(p, "Image1", "Box");
            EcoreProcessUtils.mapCapInputToProcessVar(p.getVariables(), s1, s2);

            p.getSteps().add(s1);
            p.getSteps().add(s2);
            return p;
        }
        return null;
    }
}
