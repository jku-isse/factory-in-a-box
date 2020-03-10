package fiab.mes.order.ecore;

import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import ProcessCore.AbstractCapability;
import ProcessCore.CapabilityInvocation;
import ProcessCore.ProcessCoreFactory;
import ProcessCore.XmlRoot;
import fiab.mes.capabilities.plotting.EcoreProcessUtils;
import fiab.mes.capabilities.plotting.WellknownPlotterCapability;
import fiab.mes.capabilities.plotting.WellknownPlotterCapability.SupportedColors;

public class ProduceProcess {

	@Test
	void produceSimpleProcess() throws IOException {
		String prefix="step-";
		
		XmlRoot root = ProcessCoreFactory.eINSTANCE.createXmlRoot();
		root.setDisplayName("ProcessTemplate4Plotters");
		root.getProcesses().add(getSequential4ColorProcess(prefix, root));		
		FileDataPersistor fdp = new FileDataPersistor("ProcessTemplate4Plotters");
		fdp.persistShopfloorData(Arrays.asList(root));
		
	}

	@Test
	void produceCapabilitiesOnly() throws IOException {
	
		XmlRoot root = ProcessCoreFactory.eINSTANCE.createXmlRoot();
		root.setDisplayName("Capabilities");
		
		// first four supported colors: BLACK, BLUE, GREEN, RED,
		for (SupportedColors color : WellknownPlotterCapability.SupportedColors.values()) {
			AbstractCapability cap = WellknownPlotterCapability.getColorPlottingCapability(color);
			root.getCapabilities().add(cap);
		}
		
		FileDataPersistor fdp = new FileDataPersistor("Capabilities");
		fdp.persistShopfloorData(Arrays.asList(root));
		
	}
	
	public static ProcessCore.Process getSequential4ColorProcess(String prefix) {
		return getSequential4ColorProcess(prefix, ProcessCoreFactory.eINSTANCE.createXmlRoot());
	}
	
	public static ProcessCore.Process getSequential4ColorProcess(String prefix, XmlRoot root) {
		// first four supported colors: BLACK, BLUE, GREEN, RED,
		for (SupportedColors color : WellknownPlotterCapability.SupportedColors.values()) {
			AbstractCapability cap = WellknownPlotterCapability.getColorPlottingCapability(color);
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
		p.setDisplayName("ProcessTemplate4Plotters");
		p.setID("ProcessTemplate4Plotters");
		EcoreProcessUtils.addProcessvariables(p, "Image1", "Image2", "Image3", "Image4");
		EcoreProcessUtils.mapCapInputToProcessVar(p.getVariables(), s1,s2,s3,s4);
		
		p.getSteps().add(s1);
		p.getSteps().add(s2);
		p.getSteps().add(s3);
		p.getSteps().add(s4);
		return p;
	}
	
	public static ProcessCore.Process getSingleGreenStepProcess(String prefix) {
		return getSequentialStepProcess(prefix, ProcessCoreFactory.eINSTANCE.createXmlRoot(), SupportedColors.GREEN);
	}
	
	public static ProcessCore.Process getSingleRedStepProcess(String prefix) {
		return getSequentialStepProcess(prefix, ProcessCoreFactory.eINSTANCE.createXmlRoot(), SupportedColors.RED);
	}
	
	public static ProcessCore.Process getRedAndGreenStepProcess(String prefix) {
		return getSequentialStepProcess(prefix, ProcessCoreFactory.eINSTANCE.createXmlRoot(), SupportedColors.RED, SupportedColors.GREEN);
	}
	
	public static ProcessCore.Process getSequentialStepProcess(String prefix, XmlRoot root, SupportedColors... colors) {
		// first four supported colors: BLACK, BLUE, GREEN, RED,
		ProcessCore.Process p = ProcessCoreFactory.eINSTANCE.createProcess();
		p.setDisplayName(prefix+"SequentialProcess");
		p.setID(prefix+"SequentialProcess");
		
		int count = 0;
		for (SupportedColors color : colors) {
			AbstractCapability cap = WellknownPlotterCapability.getColorPlottingCapability(color);
			root.getCapabilities().add(cap);		

			CapabilityInvocation s1 = ProcessCoreFactory.eINSTANCE.createCapabilityInvocation();		
			s1.setID(prefix+count);		
			s1.setDisplayName(root.getCapabilities().get(count).getDisplayName());		
			s1.setInvokedCapability(root.getCapabilities().get(count));				
			s1.getInputMappings().add(EcoreProcessUtils.getVariableMapping(root.getCapabilities().get(count).getInputs().get(0)));
			EcoreProcessUtils.addProcessvariables(p, "Image"+count);
			EcoreProcessUtils.mapCapInputToProcessVar(p.getVariables(), s1);
			p.getSteps().add(s1);
		}
		return p;
	}
}
