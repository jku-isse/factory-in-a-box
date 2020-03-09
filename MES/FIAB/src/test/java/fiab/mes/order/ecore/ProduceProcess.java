package fiab.mes.order.ecore;

import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import ProcessCore.AbstractCapability;
import ProcessCore.CapabilityInvocation;
import ProcessCore.ProcessCoreFactory;
import ProcessCore.XmlRoot;

class ProduceProcess {

	@Test
	void produceSimpleProcess() throws IOException {
		String prefix="step-";
		
		XmlRoot root = ProcessCoreFactory.eINSTANCE.createXmlRoot();
		root.setDisplayName("ProcessTemplate4Plotters");
		
		AbstractCapability red = EcoreProcessUtils.getColorPlotCapability("Red");
		AbstractCapability blue = EcoreProcessUtils.getColorPlotCapability("Blue");
		AbstractCapability green = EcoreProcessUtils.getColorPlotCapability("Green");
		AbstractCapability yellow = EcoreProcessUtils.getColorPlotCapability("Yellow");
		root.getCapabilities().add(red);
		root.getCapabilities().add(blue);
		root.getCapabilities().add(green);
		root.getCapabilities().add(yellow);
		
		CapabilityInvocation s1 = ProcessCoreFactory.eINSTANCE.createCapabilityInvocation();
		CapabilityInvocation s2 = ProcessCoreFactory.eINSTANCE.createCapabilityInvocation();
		CapabilityInvocation s3 = ProcessCoreFactory.eINSTANCE.createCapabilityInvocation();
		CapabilityInvocation s4 = ProcessCoreFactory.eINSTANCE.createCapabilityInvocation();
		s1.setID(prefix+"1");
		s2.setID(prefix+"2");
		s3.setID(prefix+"3");
		s4.setID(prefix+"4");
		s1.setDisplayName("red plotting");
		s2.setDisplayName("blue plotting");
		s3.setDisplayName("green plotting");
		s4.setDisplayName("yellow plotting");
		s1.setInvokedCapability(red);
		s2.setInvokedCapability(blue);		
		s3.setInvokedCapability(green);		
		s4.setInvokedCapability(yellow);		
		s1.getInputMappings().add(EcoreProcessUtils.getVariableMapping(red.getInputs().get(0)));
		s2.getInputMappings().add(EcoreProcessUtils.getVariableMapping(blue.getInputs().get(0)));
		s3.getInputMappings().add(EcoreProcessUtils.getVariableMapping(green.getInputs().get(0)));
		s4.getInputMappings().add(EcoreProcessUtils.getVariableMapping(yellow.getInputs().get(0)));
		
		ProcessCore.Process p = ProcessCoreFactory.eINSTANCE.createProcess();
		p.setDisplayName("ProcessTemplate4Plotters");
		p.setID("ProcessTemplate4Plotters");
		EcoreProcessUtils.addProcessvariables(p, "Image1", "Image2", "Image3", "Image4");
		EcoreProcessUtils.mapCapInputToProcessVar(p.getVariables(), s1,s2,s3,s4);
		
		p.getSteps().add(s1);
		p.getSteps().add(s2);
		p.getSteps().add(s3);
		p.getSteps().add(s4);
		root.getProcesses().add(p);		
		
		FileDataPersistor fdp = new FileDataPersistor("ProcessTemplate4Plotters");
		fdp.persistShopfloorData(Arrays.asList(root));
		
	}

}
